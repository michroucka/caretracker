import { screen, waitFor } from '@testing-library/react';
import { delay, http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { ClientCreate } from './ClientCreate';
import { renderWithProviders } from '@/test/render';
import { serveAndRecord, serverError } from '@/test/hooks';
import { apiUrl, server } from '@/test/server';
import { admin } from '@/test/users';
import { toIsoDate } from '@/lib/dates';

const PLZEN = { id: 2, city: 'Plzeň' };
const KLATOVY = { id: 3, city: 'Klatovy' };

const SLOVANY = { id: 5, name: 'DPS Slovany', street: 'Slovanská 3', city: 'Plzeň', postalCode: '30100', active: true };
const BORY = { id: 6, name: 'DPS Bory', street: 'Borská 8', city: 'Plzeň', postalCode: '30200', active: true };
const CLOSED_HOUSE = { id: 7, name: 'DPS Zavřený', street: 'Stará 1', city: 'Plzeň', postalCode: '30100', active: false };

const NEXT_PERSONAL_NUMBERS = { [PLZEN.id]: '020043', [KLATOVY.id]: '030007' };

const firstOfThisMonth = () => {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
};

function serveFormOptions() {
    server.use(
        http.get(apiUrl('/departments'), () => HttpResponse.json([PLZEN, KLATOVY])),
        http.get(apiUrl('/employees'), () => HttpResponse.json([
            { id: 10, firstName: 'Jana', lastName: 'Testová', department: PLZEN },
            { id: 11, firstName: 'Petr', lastName: 'Svoboda', department: KLATOVY },
        ])),
        http.get(apiUrl('/tasks'), () => HttpResponse.json([{ id: 40, name: 'Nákup' }, { id: 41, name: 'Úklid' }])),
        http.get(apiUrl('/departments/:id/next-personal-number'), ({ params }) =>
            HttpResponse.json({ personalNumber: NEXT_PERSONAL_NUMBERS[params.id] })),
        http.get(apiUrl('/departments/:id/municipalities'), () => HttpResponse.json([{ id: 1, name: 'Dobřany' }, { id: 2, name: 'Plzeň' }])),
        http.get(apiUrl('/departments/:id/supported-housings'), () => HttpResponse.json([SLOVANY, BORY, CLOSED_HOUSE])),
    );
}

async function chooseDepartment(user, name) {
    await user.click(field('Středisko'));
    await user.click(screen.getByRole('option', { name }));
}

// handlers passed here override the default form options, server.use gives the last registered handler priority
function renderCreateForm(...overrides) {
    serveFormOptions();
    server.use(...overrides);
    return renderWithProviders(
        <Routes>
            <Route path="/clients/new" element={<ClientCreate />} />
            <Route path="/clients/:clientId" element={<h1>Detail nového klienta</h1>} />
        </Routes>,
        { authStatus: admin(), route: '/clients/new' },
    );
}

const field = (label) => screen.getByLabelText(new RegExp(`^${label}`));
const submitButton = () => screen.getByRole('button', { name: 'Vytvořit klienta' });

async function pickFirstDayOfThisMonth(user) {
    await user.click(field('Datum narození'));
    const dayLabel = new Intl.DateTimeFormat('cs-CZ', { day: 'numeric', month: 'long', year: 'numeric' }).format(firstOfThisMonth());
    await user.click(screen.getByRole('button', { name: new RegExp(`, ${dayLabel}$`) }));
}

// pasting instead of typing key by key keeps the long form fast enough for slower CI runners
async function enterText(user, element, text) {
    await user.click(element);
    await user.paste(text);
}

async function enterCity(user, city) {
    await user.click(field('Město'));
    await user.type(screen.getByPlaceholderText('Hledat obec…'), city);
    await user.click(screen.getByRole('option', { name: new RegExp(city) }));
}

async function chooseOption(user, label, optionName) {
    await user.click(field(label));
    await user.click(screen.getByRole('option', { name: optionName }));
}

const chooseSupportedHousing = async (user, name) => {
    await chooseOption(user, 'Druh ubytování', 'Dům s pečovatelskou službou (DPS)');
    await chooseOption(user, 'Dům s pečovatelskou službou', name);
};

async function fillRequiredFields(user) {
    await enterText(user, field('Jméno'), ' Marie ');
    await enterText(user, field('Příjmení'), 'Nová');
    await user.click(field('Pohlaví'));
    await user.click(screen.getByRole('option', { name: 'Žena' }));
    await pickFirstDayOfThisMonth(user);
    await user.click(field('Klíčový pracovník'));
    await user.click(screen.getByRole('option', { name: 'Jana Testová' }));
    await enterText(user, field('Ulice a číslo popisné'), 'Americká 12');
    await enterCity(user, 'Plzeň');
    await enterText(user, field('PSČ'), '30100');
}

describe('ClientCreate', () => {
    it('requires the mandatory fields and sends nothing', async () => {
        const creations = serveAndRecord('post', '/clients');
        const { user } = renderCreateForm();

        await user.click(await screen.findByRole('button', { name: 'Vytvořit klienta' }));

        for (const message of [
            'Prosím zadejte jméno',
            'Prosím zadejte příjmení',
            'Prosím vyberte pohlaví',
            'Prosím zadejte datum narození',
            'Prosím vyberte klíčového pracovníka',
            'Prosím zadejte ulici a číslo popisné',
            'Prosím zadejte město',
            'Prosím zadejte PSČ',
        ]) {
            expect(screen.getByText(message)).toBeInTheDocument();
        }
        // the admin's own department is preselected
        expect(screen.queryByText('Prosím vyberte středisko')).not.toBeInTheDocument();
        expect(field('Jméno')).toHaveAttribute('aria-invalid', 'true');
        expect(field('Jméno')).toHaveFocus();
        expect(creations).toHaveLength(0);
    });

    it('checks the format of postal code, phone, e-mail and personal number', async () => {
        const creations = serveAndRecord('post', '/clients');
        const { user } = renderCreateForm();
        await screen.findByRole('button', { name: 'Vytvořit klienta' });
        await fillRequiredFields(user);

        await user.clear(field('PSČ'));
        await enterText(user, field('PSČ'), '301');
        await enterText(user, field('Telefon'), '12345');
        await enterText(user, field('E-mail'), 'marie@seznam');
        await user.clear(field('Osobní číslo'));
        await enterText(user, field('Osobní číslo'), '42');
        await user.click(submitButton());

        expect(screen.getByText('PSČ ve tvaru 123 45')).toBeInTheDocument();
        expect(screen.getByText('Telefon ve tvaru 123 456 789 nebo +420 123 456 789')).toBeInTheDocument();
        expect(screen.getByText('Zadejte platnou e-mailovou adresu')).toBeInTheDocument();
        expect(screen.getByText('Osobní číslo musí mít 6 číslic')).toBeInTheDocument();
        expect(creations).toHaveLength(0);
    });

    it('keeps the personal number to at most six digits', async () => {
        const { user } = renderCreateForm();
        await waitFor(() => expect(field('Osobní číslo')).toHaveValue('020043'));

        await user.clear(field('Osobní číslo'));
        await user.type(field('Osobní číslo'), '12a34567');

        expect(field('Osobní číslo')).toHaveValue('123456');
    });

    it('prefills the next personal number of the preselected department', async () => {
        renderCreateForm();

        await waitFor(() => expect(field('Osobní číslo')).toHaveValue('020043'));
    });

    it('prefills the personal number of a newly chosen department', async () => {
        const { user } = renderCreateForm();
        await waitFor(() => expect(field('Osobní číslo')).toHaveValue('020043'));

        await chooseDepartment(user, 'Klatovy');

        await waitFor(() => expect(field('Osobní číslo')).toHaveValue('030007'));
    });

    it('keeps a personal number the user typed when the department changes', async () => {
        const { user } = renderCreateForm();
        await waitFor(() => expect(field('Osobní číslo')).toHaveValue('020043'));
        await user.clear(field('Osobní číslo'));
        await enterText(user, field('Osobní číslo'), '029999');

        await chooseDepartment(user, 'Klatovy');

        expect(field('Osobní číslo')).toHaveValue('029999');
    });

    it('ignores a late number of a department that is no longer chosen', async () => {
        const { user } = renderCreateForm(http.get(apiUrl('/departments/:id/next-personal-number'), async ({ params }) => {
            if (Number(params.id) === PLZEN.id) await delay(200);
            return HttpResponse.json({ personalNumber: NEXT_PERSONAL_NUMBERS[params.id] });
        }));
        await screen.findByRole('button', { name: 'Vytvořit klienta' });

        await chooseDepartment(user, 'Klatovy');
        await waitFor(() => expect(field('Osobní číslo')).toHaveValue('030007'));
        await new Promise((resolve) => setTimeout(resolve, 300));

        expect(field('Osobní číslo')).toHaveValue('030007');
    });

    it('leaves the personal number empty when the next number cannot be loaded', async () => {
        const creations = serveAndRecord('post', '/clients', () => HttpResponse.json({ id: 55 }));
        const { user } = renderCreateForm(http.get(apiUrl('/departments/:id/next-personal-number'), serverError('Chyba serveru', 500)));
        await screen.findByRole('button', { name: 'Vytvořit klienta' });
        await fillRequiredFields(user);

        await user.click(submitButton());

        await waitFor(() => expect(creations).toHaveLength(1));
        expect(creations[0].body.personalNumber).toBeNull();
    });

    it('offers only caregivers of the chosen department', async () => {
        const { user } = renderCreateForm();
        await screen.findByRole('button', { name: 'Vytvořit klienta' });

        await user.click(field('Klíčový pracovník'));
        expect(screen.getByRole('option', { name: 'Jana Testová' })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: 'Petr Svoboda' })).not.toBeInTheDocument();
    });

    it('sends the filled form as JSON and opens the new client', async () => {
        const creations = serveAndRecord('post', '/clients', () => HttpResponse.json({ id: 55, firstName: 'Marie', lastName: 'Nová' }));
        const { user } = renderCreateForm();
        await screen.findByRole('button', { name: 'Vytvořit klienta' });
        await fillRequiredFields(user);

        await enterText(user, field('Telefon'), '+420777123456');
        await enterText(user, field('E-mail'), 'marie@example.cz');
        await user.clear(field('Osobní číslo'));
        await enterText(user, field('Osobní číslo'), '010042');
        await user.click(field('Úkony'));
        await user.click(screen.getByRole('option', { name: 'Úklid' }));
        await user.keyboard('{Escape}');
        await user.click(submitButton());

        expect(await screen.findByRole('heading', { name: 'Detail nového klienta' })).toBeInTheDocument();
        expect(window.location.pathname).toBe('/clients/55');
        expect(screen.getByText('Klient úspěšně vytvořen')).toBeInTheDocument();
        expect(creations).toHaveLength(1);
        expect(creations[0].body).toEqual({
            firstName: 'Marie',
            lastName: 'Nová',
            gender: 'FEMALE',
            dateOfBirth: toIsoDate(firstOfThisMonth()),
            personalNumber: '010042',
            departmentId: 2,
            caregiverId: 10,
            street: 'Americká 12',
            city: 'Plzeň',
            postalCode: '301 00',
            accommodationType: null,
            supportedHousingId: null,
            travelDistanceKm: null,
            hasElevator: false,
            floorOrRoom: null,
            accessMethod: null,
            lockCode: null,
            accessNote: null,
            email: 'marie@example.cz',
            phone: '+420 777 123 456',
            legallyCompetent: true,
            benefits: 'NONE',
            taskIds: [41],
            relativesContact: null,
            generalPractitioner: null,
            notes: null,
            terminationDate: null,
            terminationReason: null,
        });
    });

    it('shows the server message and stays on the form when saving fails', async () => {
        serveAndRecord('post', '/clients', serverError('Klient s tímto osobním číslem už existuje', 409));
        const { user } = renderCreateForm();
        await screen.findByRole('button', { name: 'Vytvořit klienta' });
        await fillRequiredFields(user);

        await user.click(submitButton());

        expect(await screen.findByText('Chyba při vytváření klienta')).toBeInTheDocument();
        expect(screen.getByText('Klient s tímto osobním číslem už existuje')).toBeInTheDocument();
        expect(window.location.pathname).toBe('/clients/new');
        await waitFor(() => expect(submitButton()).toBeEnabled());
        expect(field('Jméno')).toHaveValue(' Marie ');
    });

    describe('accommodation', () => {
        it('offers the municipalities of the department and accepts a city that is not among them', async () => {
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });

            await user.click(field('Město'));
            expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(['Dobřany', 'Plzeň']);

            await user.type(screen.getByPlaceholderText('Hledat obec…'), 'Nepomuk');
            await user.click(screen.getByRole('option', { name: /Nepomuk/ }));

            expect(field('Město')).toHaveTextContent('Nepomuk');
        });

        it('loads the municipalities of a newly chosen department', async () => {
            const { user } = renderCreateForm(http.get(apiUrl('/departments/:id/municipalities'), ({ params }) =>
                HttpResponse.json(Number(params.id) === KLATOVY.id ? [{ id: 9, name: 'Janovice' }] : [{ id: 1, name: 'Dobřany' }])));
            await screen.findByRole('button', { name: 'Vytvořit klienta' });

            await chooseDepartment(user, 'Klatovy');
            await user.click(field('Město'));

            expect(await screen.findByRole('option', { name: 'Janovice' })).toBeInTheDocument();
            expect(screen.queryByRole('option', { name: 'Dobřany' })).not.toBeInTheDocument();
        });

        it('offers only active supported housings, and only for the supported housing type', async () => {
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });
            expect(screen.queryByLabelText(/^Dům s pečovatelskou službou/)).not.toBeInTheDocument();

            await chooseOption(user, 'Druh ubytování', 'Dům s pečovatelskou službou (DPS)');
            await user.click(field('Dům s pečovatelskou službou'));

            await waitFor(() => expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(['DPS Slovany', 'DPS Bory']));
        });

        it('fills the address from the chosen house and locks it', async () => {
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });

            await chooseSupportedHousing(user, 'DPS Slovany');

            expect(field('Ulice a číslo popisné')).toHaveValue('Slovanská 3');
            expect(field('Město')).toHaveValue('Plzeň');
            expect(field('PSČ')).toHaveValue('301 00');
            for (const label of ['Ulice a číslo popisné', 'Město', 'PSČ']) expect(field(label)).toHaveAttribute('readonly');

            await chooseOption(user, 'Dům s pečovatelskou službou', 'DPS Bory');
            expect(field('Ulice a číslo popisné')).toHaveValue('Borská 8');
        });

        it('unlocks the address and keeps its values when the type changes from supported housing', async () => {
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });
            await chooseSupportedHousing(user, 'DPS Slovany');

            await chooseOption(user, 'Druh ubytování', 'Nájemní byt');

            expect(field('Ulice a číslo popisné')).not.toHaveAttribute('readonly');
            expect(field('Ulice a číslo popisné')).toHaveValue('Slovanská 3');
            expect(screen.queryByLabelText(/^Dům s pečovatelskou službou/)).not.toBeInTheDocument();
            expect(field('Město')).toHaveTextContent('Plzeň');
        });

        it('requires a house for supported housing, but not the address', async () => {
            const creations = serveAndRecord('post', '/clients');
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });
            await chooseOption(user, 'Druh ubytování', 'Dům s pečovatelskou službou (DPS)');

            await user.click(submitButton());

            expect(screen.getByText('Vyberte dům s pečovatelskou službou')).toBeInTheDocument();
            expect(screen.queryByText('Prosím zadejte ulici a číslo popisné')).not.toBeInTheDocument();
            expect(creations).toHaveLength(0);
        });

        it('forgets the chosen house when the department changes', async () => {
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });
            await chooseSupportedHousing(user, 'DPS Slovany');

            await chooseDepartment(user, 'Klatovy');

            expect(field('Dům s pečovatelskou službou')).toHaveTextContent('Vyberte dům');
        });

        it('sends the accommodation fields', async () => {
            const creations = serveAndRecord('post', '/clients', () => HttpResponse.json({ id: 55 }));
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });
            await fillRequiredFields(user);
            await chooseSupportedHousing(user, 'DPS Slovany');
            await user.click(field('Výtah v domě'));
            await enterText(user, field('Patro nebo pokoj'), '2. patro');
            await chooseOption(user, 'Přístup do bytu', 'Klíče');
            await enterText(user, field('Kód od zámku'), '1234');
            await enterText(user, field('Dojezdová vzdálenost'), '12');

            await user.click(submitButton());

            await waitFor(() => expect(creations).toHaveLength(1));
            expect(creations[0].body).toMatchObject({
                accommodationType: 'SUPPORTED_HOUSING',
                supportedHousingId: 5,
                street: 'Slovanská 3',
                postalCode: '301 00',
                hasElevator: true,
                floorOrRoom: '2. patro',
                accessMethod: 'KEYS',
                lockCode: '1234',
                travelDistanceKm: 12,
            });
        });

        it('rejects a negative travel distance', async () => {
            const creations = serveAndRecord('post', '/clients');
            const { user } = renderCreateForm();
            await screen.findByRole('button', { name: 'Vytvořit klienta' });
            await fillRequiredFields(user);
            await enterText(user, field('Dojezdová vzdálenost'), '-3');

            await user.click(submitButton());

            expect(screen.getByText('Dojezdová vzdálenost nesmí být záporná')).toBeInTheDocument();
            expect(creations).toHaveLength(0);
        });
    });
});
