import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { DepartmentSheet } from './DepartmentSheet';
import { serveAndRecord, serverError } from '@/test/hooks';
import { renderWithProviders } from '@/test/render';
import { apiUrl, server } from '@/test/server';
import { admin, caregiver, coordinator } from '@/test/users';

const PLZEN = { id: 2, city: 'Plzeň', street: 'Americká 12', postalCode: '301 00', departmentNumber: 7, coordinator: null, active: true };

const SLOVANY = { id: 5, name: 'DPS Slovany', street: 'Slovanská 3', city: 'Plzeň', postalCode: '301 00', active: true };
const CLOSED_HOUSE = { id: 6, name: 'DPS Zavřený', street: 'Stará 1', city: 'Plzeň', postalCode: '302 00', active: false };

/** Serves both collections from variables, so a reload after a change shows what the server stored. */
function serveCollections({ municipalities = [{ id: 1, name: 'Dobřany' }, { id: 2, name: 'Nýřany' }], housings = [SLOVANY, CLOSED_HOUSE] } = {}) {
    const stored = { municipalities, housings };
    server.use(
        http.get(apiUrl('/departments/:id/municipalities'), () => HttpResponse.json(stored.municipalities)),
        http.get(apiUrl('/departments/:id/supported-housings'), () => HttpResponse.json(stored.housings)),
    );
    return stored;
}

function renderSheet(authStatus = admin(), department = PLZEN) {
    return renderWithProviders(
        <DepartmentSheet open onOpenChange={() => {}} department={department} isLoading={false} employees={[]} onSave={() => {}} onTerminate={() => {}} onActivate={() => {}} />,
        { authStatus },
    );
}

describe('DepartmentSheet – municipalities', () => {
    it('lists the municipalities of the department', async () => {
        serveCollections();

        renderSheet();

        const list = await screen.findByRole('list', { name: 'Obce střediska' });
        expect(within(list).getAllByRole('listitem').map((item) => item.textContent)).toEqual(['Dobřany', 'Nýřany']);
    });

    it('adds a municipality and shows the stored list', async () => {
        const stored = serveCollections();
        const creations = serveAndRecord('post', '/departments/:id/municipalities', () => {
            stored.municipalities = [...stored.municipalities, { id: 3, name: 'Chotěšov' }];
            return HttpResponse.json({ id: 3, name: 'Chotěšov' });
        });
        const { user } = renderSheet();
        await screen.findByText('Dobřany');

        await user.type(screen.getByRole('textbox', { name: 'Název obce' }), '  Chotěšov ');
        await user.click(screen.getByRole('button', { name: 'Přidat obec' }));

        expect(await screen.findByText('Chotěšov')).toBeInTheDocument();
        expect(creations[0].path).toBe('/api/departments/2/municipalities');
        expect(creations[0].body).toEqual({ name: 'Chotěšov' });
        expect(screen.getByRole('textbox', { name: 'Název obce' })).toHaveValue('');
    });

    it('asks for a name without sending anything', async () => {
        serveCollections();
        const creations = serveAndRecord('post', '/departments/:id/municipalities');
        const { user } = renderSheet();
        await screen.findByText('Dobřany');

        await user.click(screen.getByRole('button', { name: 'Přidat obec' }));

        expect(screen.getByText('Zadejte název obce')).toBeInTheDocument();
        expect(creations).toHaveLength(0);
    });

    it('shows the server message when the municipality already exists', async () => {
        serveCollections();
        serveAndRecord('post', '/departments/:id/municipalities', serverError('Obec už ve středisku existuje', 409));
        const { user } = renderSheet();
        await screen.findByText('Dobřany');

        await user.type(screen.getByRole('textbox', { name: 'Název obce' }), 'Dobřany');
        await user.click(screen.getByRole('button', { name: 'Přidat obec' }));

        expect(await screen.findByText('Obec už ve středisku existuje')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Název obce' })).toHaveValue('Dobřany');
    });

    it('removes a municipality', async () => {
        const stored = serveCollections();
        const deletions = serveAndRecord('delete', '/departments/:id/municipalities/:municipalityId', () => {
            stored.municipalities = stored.municipalities.filter((municipality) => municipality.id !== 1);
            return new HttpResponse(null, { status: 204 });
        });
        const { user } = renderSheet();

        await user.click(await screen.findByRole('button', { name: 'Odebrat obec Dobřany' }));

        await waitFor(() => expect(screen.queryByText('Dobřany')).not.toBeInTheDocument());
        expect(deletions[0].path).toBe('/api/departments/2/municipalities/1');
    });

    it.each([
        ['a caregiver', caregiver()],
        ['a coordinator of another department', coordinator({ departmentId: 3 })],
    ])('is read-only for %s', async (_, authStatus) => {
        serveCollections();

        renderSheet(authStatus);

        expect(await screen.findByText('Dobřany')).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Název obce' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Odebrat obec/ })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Přidat dům' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Ukončit dům/ })).not.toBeInTheDocument();
    });

    it('lets the coordinator of the department manage it', async () => {
        serveCollections();

        renderSheet(coordinator({ departmentId: 2 }));

        expect(await screen.findByRole('textbox', { name: 'Název obce' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Přidat dům' })).toBeInTheDocument();
    });
});

describe('DepartmentSheet – supported housings', () => {
    it('lists the houses and marks the terminated one', async () => {
        serveCollections();

        renderSheet();

        const list = await screen.findByRole('list', { name: 'Domy s pečovatelskou službou' });
        const [active, closed] = within(list).getAllByRole('listitem');
        expect(active).toHaveTextContent('DPS Slovany');
        expect(active).toHaveTextContent('Slovanská 3, 301 00 Plzeň');
        expect(within(active).queryByText('Ukončený')).not.toBeInTheDocument();
        expect(within(closed).getByText('Ukončený')).toBeInTheDocument();
        expect(within(closed).queryByRole('button', { name: /Ukončit dům/ })).not.toBeInTheDocument();
    });

    it('validates and adds a house', async () => {
        const stored = serveCollections();
        const creations = serveAndRecord('post', '/departments/:id/supported-housings', ({ request }) => request.clone().json().then((body) => {
            stored.housings = [...stored.housings, { id: 9, ...body, active: true }];
            return HttpResponse.json({ id: 9, ...body, active: true });
        }));
        const { user } = renderSheet();
        await user.click(await screen.findByRole('button', { name: 'Přidat dům' }));

        await user.click(screen.getByRole('button', { name: 'Uložit dům' }));
        for (const message of ['Prosím zadejte název domu', 'Prosím zadejte ulici a číslo popisné', 'Prosím zadejte město', 'Prosím zadejte PSČ']) {
            expect(screen.getByText(message)).toBeInTheDocument();
        }
        expect(creations).toHaveLength(0);

        await user.type(screen.getByRole('textbox', { name: 'Název domu' }), 'DPS Bory');
        await user.type(screen.getByRole('textbox', { name: 'Ulice a číslo popisné' }), 'Borská 8');
        await user.type(screen.getByRole('textbox', { name: 'Město' }), 'Plzeň');
        await user.type(screen.getByRole('textbox', { name: 'PSČ' }), '30200');
        await user.click(screen.getByRole('button', { name: 'Uložit dům' }));

        expect(await screen.findByText('DPS Bory')).toBeInTheDocument();
        expect(creations[0].path).toBe('/api/departments/2/supported-housings');
        expect(creations[0].body).toEqual({ name: 'DPS Bory', street: 'Borská 8', city: 'Plzeň', postalCode: '302 00' });
        expect(screen.queryByRole('textbox', { name: 'Název domu' })).not.toBeInTheDocument();
    });

    it('edits a house', async () => {
        serveCollections();
        const updates = serveAndRecord('put', '/departments/:id/supported-housings/:housingId', ({ request }) => request.clone().json().then((body) => HttpResponse.json({ id: 5, ...body, active: true })));
        const { user } = renderSheet();

        await user.click(await screen.findByRole('button', { name: 'Upravit dům DPS Slovany' }));
        expect(screen.getByRole('textbox', { name: 'Název domu' })).toHaveValue('DPS Slovany');
        await user.clear(screen.getByRole('textbox', { name: 'Ulice a číslo popisné' }));
        await user.type(screen.getByRole('textbox', { name: 'Ulice a číslo popisné' }), 'Slovanská 5');
        await user.click(screen.getByRole('button', { name: 'Uložit dům' }));

        await waitFor(() => expect(updates).toHaveLength(1));
        expect(updates[0].path).toBe('/api/departments/2/supported-housings/5');
        expect(updates[0].body).toEqual({ name: 'DPS Slovany', street: 'Slovanská 5', city: 'Plzeň', postalCode: '301 00' });
    });

    it('terminates a house after confirmation', async () => {
        const stored = serveCollections();
        const terminations = serveAndRecord('put', '/departments/:id/supported-housings/:housingId/terminate', () => {
            stored.housings = stored.housings.map((housing) => ({ ...housing, active: false }));
            return HttpResponse.json({ ...SLOVANY, active: false });
        });
        const { user } = renderSheet();

        await user.click(await screen.findByRole('button', { name: 'Ukončit dům DPS Slovany' }));
        await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Ukončit' }));

        await waitFor(() => expect(terminations).toHaveLength(1));
        expect(terminations[0].path).toBe('/api/departments/2/supported-housings/5/terminate');
        await waitFor(() => expect(screen.queryByRole('button', { name: /Ukončit dům/ })).not.toBeInTheDocument());
    });
});
