import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ClientDetail } from './ClientDetail';
import { renderWithProviders } from '@/test/render';
import { serveAndRecord, serverError } from '@/test/hooks';
import { apiUrl, server } from '@/test/server';
import { admin, caregiver, coordinator } from '@/test/users';

const PLZEN = { id: 2, city: 'Plzeň' };

const MARIE = {
    id: 7,
    firstName: 'Marie',
    lastName: 'Nová',
    gender: 'FEMALE',
    dateOfBirth: '1940-03-15',
    personalNumber: '010034',
    legallyCompetent: true,
    street: 'Americká 12',
    city: 'Plzeň',
    postalCode: '30100',
    phone: '777123456',
    email: 'marie@example.cz',
    relativesContact: 'Dcera Jana, 603 111 222',
    generalPractitioner: null,
    notes: null,
    benefits: 'TWO',
    active: true,
    created: '2024-05-01',
    pictureId: null,
    latitude: null,
    longitude: null,
    department: PLZEN,
    caregiver: { id: 10, fullName: 'Jana Testová' },
    tasks: [{ id: 40, name: 'Nákup' }, { id: 41, name: 'Úklid' }],
    terminationDate: null,
    terminationReason: null,
};

const ACCOMMODATION = {
    accommodationType: 'SUPPORTED_HOUSING',
    supportedHousing: { id: 5, name: 'DPS Slovany' },
    travelDistanceKm: 12,
    hasElevator: true,
    floorOrRoom: '2. patro',
    accessMethod: 'KEYS',
    lockCode: '4821',
    accessNote: 'Klíče u vrátného',
};

const TERMINATED_MARIE = { ...MARIE, active: false, terminationDate: '2026-10-31', terminationReason: 'EXITUS' };

/** Serves the client from a variable, so a reload after an action shows what the server changed. */
function serveClient(initialClient, { housingActive = true, documents = [] } = {}) {
    let storedClient = initialClient;
    const documentRequests = serveAndRecord('get', '/clients/:id/documents', () => HttpResponse.json(documents));
    server.use(
        http.get(apiUrl('/clients/:id'), () => HttpResponse.json(storedClient)),
        http.get(apiUrl('/departments'), () => HttpResponse.json([PLZEN])),
        http.get(apiUrl('/employees'), () => HttpResponse.json([{ id: 10, firstName: 'Jana', lastName: 'Testová', department: PLZEN }])),
        http.get(apiUrl('/tasks'), () => HttpResponse.json([{ id: 40, name: 'Nákup' }, { id: 41, name: 'Úklid' }])),
        http.get(apiUrl('/departments/:id/municipalities'), () => HttpResponse.json([{ id: 1, name: 'Plzeň' }])),
        http.get(apiUrl('/departments/:id/supported-housings'), () => HttpResponse.json([
            { id: 5, name: 'DPS Slovany', street: 'Americká 12', city: 'Plzeň', postalCode: '30100', active: housingActive },
        ])),
    );
    const listReloads = serveAndRecord('get', '/clients', () => HttpResponse.json([]));
    return { listReloads, documentRequests, changeTo: (client) => { storedClient = client; } };
}

function renderDetail(authStatus = admin()) {
    return renderWithProviders(
        <Routes>
            <Route path="/clients/:clientId" element={<ClientDetail />} />
            <Route path="/clients" element={<h1>Seznam klientů</h1>} />
        </Routes>,
        { authStatus, route: '/clients/7' },
    );
}

const detailValue = (label) => screen.getByText(label, { selector: 'dt' }).nextElementSibling;

// the view fades out before the form appears
async function startEditing(user) {
    await user.click(await screen.findByRole('button', { name: /Upravit/ }));
    return screen.findByRole('button', { name: 'Uložit změny' });
}

async function openMoreActions(user) {
    await user.click(await screen.findByRole('button', { name: 'Další akce' }));
}

describe('ClientDetail – view', () => {
    beforeEach(() => vi.useFakeTimers({ toFake: ['Date'] }).setSystemTime(new Date(2026, 10, 9, 12)));
    afterEach(() => vi.useRealTimers());

    it("shows the client's data", async () => {
        serveClient(MARIE);

        renderDetail(caregiver());

        expect(await screen.findByRole('heading', { level: 1, name: 'Marie Nová' })).toBeInTheDocument();
        expect(screen.getByText('Aktivní')).toBeInTheDocument();
        expect(detailValue('Datum narození')).toHaveTextContent('15. 3. 1940 (86 let)');
        expect(detailValue('Pohlaví')).toHaveTextContent('Žena');
        expect(detailValue('Osobní číslo')).toHaveTextContent('010034');
        expect(detailValue('Svéprávný')).toHaveTextContent('Ano');
        expect(detailValue('Adresa')).toHaveTextContent('Americká 1230100 Plzeň');
        expect(screen.getByRole('link', { name: '777123456' })).toHaveAttribute('href', 'tel:777123456');
        expect(screen.getByRole('link', { name: 'marie@example.cz' })).toHaveAttribute('href', 'mailto:marie@example.cz');
        expect(detailValue('Kontakt na příbuzné')).toHaveTextContent('Dcera Jana, 603 111 222');
        expect(detailValue('Praktický lékař')).toHaveTextContent('–');
        expect(screen.getByText('Bez poznámek.')).toBeInTheDocument();
        expect(detailValue('Středisko')).toHaveTextContent('Plzeň');
        expect(detailValue('Klíčový pracovník')).toHaveTextContent('Jana Testová');
        expect(detailValue('Příspěvek na péči')).toHaveTextContent('II. stupeň');
        expect(screen.getByText('2 úkony')).toBeInTheDocument();
        expect(screen.getByText('Úklid')).toBeInTheDocument();
        expect(screen.queryByText('Ukončení smlouvy')).not.toBeInTheDocument();
    });

    it('shows the name with titles in the heading', async () => {
        serveClient({ ...MARIE, firstName: 'Jana', titleBefore: 'Mgr.', titleAfter: 'DiS.' });

        renderDetail(caregiver());

        expect(await screen.findByRole('heading', { level: 1, name: 'Mgr. Jana Nová, DiS.' })).toBeInTheDocument();
    });

    it('shows the guardian only of a client who is not legally competent', async () => {
        serveClient({ ...MARIE, legallyCompetent: false, guardianName: 'Jan Nový', guardianContact: '603 111 222' });

        renderDetail(caregiver());

        expect(await screen.findByText('Opatrovník', { selector: 'h2, h3, [data-slot="card-title"]' })).toBeInTheDocument();
        expect(detailValue('Kontakt na opatrovníka')).toHaveTextContent('603 111 222');
    });

    it('shows an indefinite contract without an end', async () => {
        serveClient({ ...MARIE, contractFrom: '2026-01-01', contractIndefinite: true, contractTo: null });

        renderDetail(caregiver());

        expect(await screen.findByText('Smlouva', { selector: '[data-slot="card-title"]' })).toBeInTheDocument();
        expect(detailValue('Smlouva od')).toHaveTextContent('1. 1. 2026');
        expect(detailValue('Doba smlouvy')).toHaveTextContent('Neurčitá');
    });

    it('shows the end of a fixed-term contract', async () => {
        serveClient({ ...MARIE, contractFrom: '2026-01-01', contractIndefinite: false, contractTo: '2026-12-31' });

        renderDetail(caregiver());

        expect(await screen.findByText('Doba smlouvy', { selector: 'dt' })).toBeInTheDocument();
        expect(detailValue('Doba smlouvy')).toHaveTextContent('Určitá, do 31. 12. 2026');
    });

    it('keeps the stored contract and no-charge flags when saving an unrelated change', async () => {
        serveClient({ ...MARIE, contractIndefinite: false, contractFrom: '2026-01-01', contractTo: '2026-12-31', noCharge: true });
        const updates = serveAndRecord('put', '/clients/:id', ({ request }) => request.clone().json().then((body) => HttpResponse.json({ ...MARIE, ...body })));
        const { user } = renderDetail();

        await startEditing(user);
        await user.type(screen.getByLabelText(/^Praktický lékař/), 'MUDr. Dvořák');
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        await waitFor(() => expect(updates).toHaveLength(1));
        expect(updates[0].body).toMatchObject({ contractIndefinite: false, contractFrom: '2026-01-01', contractTo: '2026-12-31', noCharge: true });
    });

    it('shows the accommodation including the lock code', async () => {
        serveClient({ ...MARIE, ...ACCOMMODATION });

        renderDetail(caregiver());

        await screen.findByRole('heading', { level: 1, name: 'Marie Nová' });
        expect(detailValue('Druh ubytování')).toHaveTextContent('Dům s pečovatelskou službou (DPS)');
        expect(detailValue('Dům s pečovatelskou službou')).toHaveTextContent('DPS Slovany');
        expect(detailValue('Patro nebo pokoj')).toHaveTextContent('2. patro');
        expect(detailValue('Výtah v domě')).toHaveTextContent('Ano');
        expect(detailValue('Přístup do bytu')).toHaveTextContent('Klíče');
        expect(detailValue('Kód od zámku')).toHaveTextContent('4821');
        expect(detailValue('Poznámka k přístupu')).toHaveTextContent('Klíče u vrátného');
        expect(detailValue('Dojezdová vzdálenost')).toHaveTextContent('12 km');
    });

    it('shows dashes for accommodation that was not filled in', async () => {
        serveClient({ ...MARIE, accommodationType: null, supportedHousing: null, travelDistanceKm: null, hasElevator: false, lockCode: null });

        renderDetail(caregiver());

        await screen.findByRole('heading', { level: 1, name: 'Marie Nová' });
        expect(detailValue('Druh ubytování')).toHaveTextContent('–');
        expect(detailValue('Kód od zámku')).toHaveTextContent('–');
        expect(detailValue('Výtah v domě')).toHaveTextContent('Ne');
        expect(screen.queryByText('Dům s pečovatelskou službou', { selector: 'dt' })).not.toBeInTheDocument();
    });

    it('shows when and why the contract of a terminated client ended', async () => {
        serveClient(TERMINATED_MARIE);

        renderDetail(caregiver());

        expect(await screen.findByText('Ukončení smlouvy')).toBeInTheDocument();
        expect(screen.getByText('Neaktivní')).toBeInTheDocument();
        expect(detailValue('Datum ukončení')).toHaveTextContent('31. 10. 2026');
        expect(detailValue('Důvod')).toHaveTextContent('Exitus');
    });

    it('says the client was not found and leads back to the list', async () => {
        serveClient(MARIE);
        server.use(http.get(apiUrl('/clients/:id'), serverError('Klient neexistuje', 404)));

        renderDetail();

        expect(await screen.findByText('Klient nebyl nalezen.')).toBeInTheDocument();
        expect(await screen.findByText('Klient nenalezen')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Zpět na klienty' })).toHaveAttribute('href', expect.stringMatching(/^\/clients/));
    });
});

describe('ClientDetail – actions by role', () => {
    it('lets a caregiver only open the individual plan', async () => {
        serveClient(MARIE);

        renderDetail(caregiver());

        expect(await screen.findByRole('link', { name: /Individuální plán/ })).toHaveAttribute('href', '/clients/7/individual-plan');
        expect(screen.queryByRole('button', { name: /Upravit/ })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Další akce' })).not.toBeInTheDocument();
    });

    it.each([
        ['coordinator', coordinator()],
        ['admin', admin()],
    ])('lets the %s edit and end the contract', async (_, authStatus) => {
        serveClient(MARIE);
        const { user } = renderDetail(authStatus);

        expect(await screen.findByRole('button', { name: /Upravit/ })).toBeInTheDocument();
        await openMoreActions(user);
        expect(screen.getByRole('menuitem', { name: /Ukončit smlouvu/ })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: /Znovu aktivovat/ })).not.toBeInTheDocument();
    });
});

describe('ClientDetail – edit', () => {
    it('saves the changed fields and shows the saved client', async () => {
        serveClient(MARIE);
        const updates = serveAndRecord('put', '/clients/:id', ({ request }) => request.clone().json().then((body) => HttpResponse.json({ ...MARIE, ...body })));
        const { user } = renderDetail();

        await startEditing(user);
        expect(screen.getByRole('heading', { level: 1, name: 'Úprava klienta' })).toBeInTheDocument();
        expect(screen.getByLabelText(/^Jméno/)).toHaveValue('Marie');
        expect(screen.getByLabelText(/^PSČ/)).toHaveValue('301 00');
        expect(screen.getByLabelText(/^Telefon/)).toHaveValue('777 123 456');

        await user.clear(screen.getByLabelText(/^Telefon/));
        await user.type(screen.getByLabelText(/^Telefon/), '603999888');
        await user.type(screen.getByLabelText(/^Praktický lékař/), 'MUDr. Dvořák');
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        await waitFor(() => expect(updates).toHaveLength(1));
        expect(updates[0].path).toMatch(/\/clients\/7$/);
        expect(updates[0].body).toMatchObject({
            firstName: 'Marie',
            phone: '603 999 888',
            generalPractitioner: 'MUDr. Dvořák',
            postalCode: '301 00',
            departmentId: 2,
            caregiverId: 10,
            taskIds: [40, 41],
            terminationDate: null,
            terminationReason: null,
        });
        expect(updates[0].body).not.toHaveProperty('picture');
        expect(await screen.findByRole('heading', { level: 1, name: 'Marie Nová' })).toBeInTheDocument();
        expect(await screen.findByRole('link', { name: '603 999 888' })).toBeInTheDocument();
        expect(screen.getByText('Klient úspěšně aktualizován')).toBeInTheDocument();
    });

    it('edits the accommodation with the house address locked', async () => {
        serveClient({ ...MARIE, ...ACCOMMODATION });
        const updates = serveAndRecord('put', '/clients/:id', ({ request }) => request.clone().json().then((body) => HttpResponse.json({ ...MARIE, ...body })));
        const { user } = renderDetail();

        await startEditing(user);

        expect(screen.getByLabelText(/^Kód od zámku/)).toHaveValue('4821');
        await waitFor(() => expect(screen.getByLabelText(/^Dům s pečovatelskou službou/)).toHaveTextContent('DPS Slovany'));
        expect(screen.getByLabelText(/^Ulice a číslo popisné/)).toHaveAttribute('readonly');

        await user.clear(screen.getByLabelText(/^Kód od zámku/));
        await user.type(screen.getByLabelText(/^Kód od zámku/), '9999');
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        await waitFor(() => expect(updates).toHaveLength(1));
        expect(updates[0].body).toMatchObject({ accommodationType: 'SUPPORTED_HOUSING', supportedHousingId: 5, lockCode: '9999', travelDistanceKm: 12, hasElevator: true });
    });

    it('keeps a terminated house of the client in the form and saves without choosing another', async () => {
        serveClient({ ...MARIE, ...ACCOMMODATION }, { housingActive: false });
        const updates = serveAndRecord('put', '/clients/:id', ({ request }) => request.clone().json().then((body) => HttpResponse.json({ ...MARIE, ...body })));
        const { user } = renderDetail();

        await startEditing(user);

        await waitFor(() => expect(screen.getByLabelText(/^Dům s pečovatelskou službou/)).toHaveTextContent('DPS Slovany (ukončený)'));
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        await waitFor(() => expect(updates).toHaveLength(1));
        expect(updates[0].body.supportedHousingId).toBe(5);
    });

    it('does not save an invalid form', async () => {
        serveClient(MARIE);
        const updates = serveAndRecord('put', '/clients/:id');
        const { user } = renderDetail();

        await startEditing(user);
        await user.clear(screen.getByLabelText(/^Jméno/));
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        expect(screen.getByText('Prosím zadejte jméno')).toBeInTheDocument();
        expect(updates).toHaveLength(0);
        expect(screen.getByRole('heading', { level: 1, name: 'Úprava klienta' })).toBeInTheDocument();
    });

    it('keeps the form open with the server message when saving fails', async () => {
        serveClient(MARIE);
        serveAndRecord('put', '/clients/:id', serverError('Neplatné PSČ'));
        const { user } = renderDetail();

        await startEditing(user);
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        expect(await screen.findByText('Chyba při aktualizaci klienta')).toBeInTheDocument();
        expect(screen.getByText('Neplatné PSČ')).toBeInTheDocument();
        expect(screen.getByRole('heading', { level: 1, name: 'Úprava klienta' })).toBeInTheDocument();
    });

    it('asks for the contract end of a terminated client', async () => {
        serveClient(TERMINATED_MARIE);
        const updates = serveAndRecord('put', '/clients/:id', () => HttpResponse.json(TERMINATED_MARIE));
        const { user } = renderDetail();

        await startEditing(user);
        expect(screen.getByLabelText(/^Datum ukončení/)).toHaveTextContent('31. 10. 2026');
        await user.click(screen.getByRole('button', { name: 'Uložit změny' }));

        await waitFor(() => expect(updates).toHaveLength(1));
        expect(updates[0].body).toMatchObject({ terminationDate: '2026-10-31', terminationReason: 'EXITUS' });
    });

    it('goes back to the view without saving on cancel', async () => {
        serveClient(MARIE);
        const updates = serveAndRecord('put', '/clients/:id');
        const { user } = renderDetail();

        await startEditing(user);
        await user.clear(screen.getByLabelText(/^Jméno/));
        await user.click(screen.getByRole('button', { name: 'Zrušit' }));

        expect(await screen.findByRole('heading', { level: 1, name: 'Marie Nová' })).toBeInTheDocument();
        expect(updates).toHaveLength(0);
    });
});

describe('ClientDetail – contract', () => {
    beforeEach(() => vi.useFakeTimers({ toFake: ['Date'] }).setSystemTime(new Date(2026, 10, 9, 12)));
    afterEach(() => vi.useRealTimers());

    it('ends the contract with a reason and a date that is not in the future', async () => {
        const { changeTo } = serveClient(MARIE);
        const terminations = serveAndRecord('put', '/clients/:id/terminate', () => {
            changeTo({ ...MARIE, active: false, terminationDate: '2026-11-05', terminationReason: 'MUTUAL_AGREEMENT' });
            return HttpResponse.json({});
        });
        const { user } = renderDetail();

        await openMoreActions(user);
        await user.click(screen.getByRole('menuitem', { name: /Ukončit smlouvu/ }));
        const dialog = screen.getByRole('dialog', { name: 'Ukončit smlouvu' });
        const date = within(dialog).getByRole('button', { name: /Datum ukončení/ });
        expect(date).toHaveTextContent('9. 11. 2026');

        await user.click(date);
        expect(screen.getByRole('button', { name: /^úterý, 10\. listopadu 2026/ })).toBeDisabled();
        await user.click(screen.getByRole('button', { name: /^čtvrtek, 5\. listopadu 2026/ }));

        await user.click(within(dialog).getByRole('button', { name: 'Ukončit smlouvu' }));
        expect(within(dialog).getByText('Prosím vyberte důvod')).toBeInTheDocument();
        expect(terminations).toHaveLength(0);

        await user.click(within(dialog).getByRole('combobox', { name: /Důvod/ }));
        await user.click(screen.getByRole('option', { name: 'Dohoda o ukončení' }));
        await user.click(within(dialog).getByRole('button', { name: 'Ukončit smlouvu' }));

        await waitFor(() => expect(terminations).toHaveLength(1));
        expect(terminations[0].path).toMatch(/\/clients\/7\/terminate$/);
        expect(terminations[0].body).toEqual({ terminationDate: '2026-11-05', terminationReason: 'MUTUAL_AGREEMENT' });
        expect(await screen.findByText('Ukončení smlouvy')).toBeInTheDocument();
        expect(detailValue('Důvod')).toHaveTextContent('Dohoda o ukončení');
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('keeps the dialog open when the server refuses to end the contract', async () => {
        serveClient(MARIE);
        serveAndRecord('put', '/clients/:id/terminate', serverError('Klient má naplánované úkony'));
        const { user } = renderDetail();

        await openMoreActions(user);
        await user.click(screen.getByRole('menuitem', { name: /Ukončit smlouvu/ }));
        const dialog = screen.getByRole('dialog', { name: 'Ukončit smlouvu' });
        await user.click(within(dialog).getByRole('combobox', { name: /Důvod/ }));
        await user.click(screen.getByRole('option', { name: 'Exitus' }));
        await user.click(within(dialog).getByRole('button', { name: 'Ukončit smlouvu' }));

        expect(await screen.findByText('Klient má naplánované úkony')).toBeInTheDocument();
        expect(screen.getByRole('dialog', { name: 'Ukončit smlouvu' })).toBeInTheDocument();
    });

    it('reactivates a terminated client', async () => {
        const { changeTo } = serveClient(TERMINATED_MARIE);
        const activations = serveAndRecord('put', '/clients/:id/activate', () => {
            changeTo(MARIE);
            return HttpResponse.json(MARIE);
        });
        const { user } = renderDetail();

        await openMoreActions(user);
        expect(screen.queryByRole('menuitem', { name: /Ukončit smlouvu/ })).not.toBeInTheDocument();
        await user.click(screen.getByRole('menuitem', { name: /Znovu aktivovat/ }));
        await user.click(screen.getByRole('button', { name: 'Aktivovat' }));

        await waitFor(() => expect(activations).toHaveLength(1));
        expect(activations[0].path).toMatch(/\/clients\/7\/activate$/);
        await waitFor(() => expect(screen.queryByText('Ukončení smlouvy')).not.toBeInTheDocument());
        expect(screen.getByText('Klient úspěšně aktivován')).toBeInTheDocument();
    });
});

const CONTRACT_DOCUMENT = {
    id: 1,
    type: 'CONTRACT',
    fileName: 'smlouva.pdf',
    contentType: 'application/pdf',
    size: 2048,
    uploadedAt: '2026-11-01T10:30:00',
    uploadedByName: 'Jana Testová',
};

const pdfFile = (name = 'souhlas.pdf', type = 'application/pdf') => new File(['x'], name, { type });

async function chooseDocumentType(user, label) {
    await user.click(await screen.findByRole('combobox', { name: 'Typ dokumentu' }));
    await user.click(await screen.findByRole('option', { name: label }));
}

describe('ClientDetail – documents', () => {
    it('does not show the documents to a caregiver and does not even ask for them', async () => {
        const { documentRequests } = serveClient(MARIE, { documents: [CONTRACT_DOCUMENT] });

        renderDetail(caregiver());

        await screen.findByRole('heading', { level: 1, name: 'Marie Nová' });
        expect(screen.queryByText('Dokumenty', { selector: '[data-slot="card-title"]' })).not.toBeInTheDocument();
        expect(documentRequests).toHaveLength(0);
    });

    it.each([['coordinator', coordinator()], ['admin', admin()]])('lists the documents for the %s', async (_, authStatus) => {
        serveClient(MARIE, { documents: [CONTRACT_DOCUMENT] });

        renderDetail(authStatus);

        expect(await screen.findByText('smlouva.pdf')).toBeInTheDocument();
        expect(screen.getByText(/Smlouva · 1\. 11\. 2026 · Jana Testová/)).toBeInTheDocument();
    });

    it('says there are no documents yet', async () => {
        serveClient(MARIE);

        renderDetail();

        expect(await screen.findByText('Klient zatím nemá žádné dokumenty.')).toBeInTheDocument();
    });

    it('uploads the chosen file with its type and shows the refreshed list', async () => {
        const { documentRequests } = serveClient(MARIE);
        const uploads = serveAndRecord('post', '/clients/:id/documents', () => HttpResponse.json(CONTRACT_DOCUMENT));
        const append = vi.spyOn(FormData.prototype, 'append');
        const { user } = renderDetail();
        await screen.findByText('Klient zatím nemá žádné dokumenty.');
        const file = pdfFile();

        await chooseDocumentType(user, 'Souhlas s klíči');
        await user.upload(screen.getByLabelText('Soubor dokumentu'), file);

        expect(await screen.findByText('Dokument nahrán')).toBeInTheDocument();
        expect(uploads).toHaveLength(1);
        expect(append).toHaveBeenCalledWith('file', file);
        expect(append).toHaveBeenCalledWith('type', 'KEY_CONSENT');
        expect(documentRequests).toHaveLength(2);
        append.mockRestore();
    });

    it('cannot pick a file before the type is chosen', async () => {
        serveClient(MARIE);

        renderDetail();

        await screen.findByText('Klient zatím nemá žádné dokumenty.');
        expect(screen.getByLabelText('Soubor dokumentu')).toBeDisabled();
    });

    it.each([
        ['a file of another type', pdfFile('text.txt', 'text/plain'), 'Nahrát lze jen PDF, JPEG nebo PNG'],
        ['a file over 5 MB', Object.defineProperty(pdfFile(), 'size', { value: 5 * 1024 * 1024 + 1 }), 'Soubor je příliš velký. Maximální velikost je 5 MB'],
    ])('refuses %s without sending it', async (_, file, message) => {
        serveClient(MARIE);
        const uploads = serveAndRecord('post', '/clients/:id/documents', () => HttpResponse.json(CONTRACT_DOCUMENT));
        const { user } = renderDetail();
        await screen.findByText('Klient zatím nemá žádné dokumenty.');

        await chooseDocumentType(user, 'Jiný');
        // user.upload would drop the file of another type because of the input's accept attribute
        fireEvent.change(screen.getByLabelText('Soubor dokumentu'), { target: { files: [file] } });

        expect(await screen.findByText(message)).toBeInTheDocument();
        expect(uploads).toHaveLength(0);
    });

    it('shows the server message when the upload fails', async () => {
        serveClient(MARIE);
        serveAndRecord('post', '/clients/:id/documents', serverError('Soubor je prázdný', 400));
        const { user } = renderDetail();
        await screen.findByText('Klient zatím nemá žádné dokumenty.');

        await chooseDocumentType(user, 'Jiný');
        await user.upload(screen.getByLabelText('Soubor dokumentu'), pdfFile());

        expect(await screen.findByText('Soubor je prázdný')).toBeInTheDocument();
    });

    it('deletes a document only after confirmation', async () => {
        const { documentRequests } = serveClient(MARIE, { documents: [CONTRACT_DOCUMENT] });
        const deletes = serveAndRecord('delete', '/clients/:id/documents/:documentId', () => new HttpResponse(null, { status: 204 }));
        const { user } = renderDetail();

        await user.click(await screen.findByRole('button', { name: 'Smazat dokument smlouva.pdf' }));
        expect(deletes).toHaveLength(0);
        const dialog = await screen.findByRole('alertdialog');
        await user.click(within(dialog).getByRole('button', { name: 'Smazat' }));

        expect(await screen.findByText('Dokument smazán')).toBeInTheDocument();
        expect(deletes[0].path).toBe('/api/clients/7/documents/1');
        expect(documentRequests).toHaveLength(2);
    });

    it('downloads a document through the API', async () => {
        serveClient(MARIE, { documents: [CONTRACT_DOCUMENT] });
        const downloads = serveAndRecord('get', '/clients/:id/documents/:documentId', () => new HttpResponse('pdf', { headers: { 'Content-Type': 'application/pdf' } }));
        URL.createObjectURL = vi.fn(() => 'blob:http://localhost/file');
        URL.revokeObjectURL = vi.fn();
        const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
        const { user } = renderDetail();

        await user.click(await screen.findByRole('button', { name: 'Stáhnout dokument smlouva.pdf' }));

        await waitFor(() => expect(click).toHaveBeenCalled());
        expect(downloads[0].path).toBe('/api/clients/7/documents/1');
        click.mockRestore();
        delete URL.createObjectURL;
        delete URL.revokeObjectURL;
    });

    it('lets a terminated client only download', async () => {
        serveClient(TERMINATED_MARIE, { documents: [CONTRACT_DOCUMENT] });

        renderDetail();

        expect(await screen.findByRole('button', { name: 'Stáhnout dokument smlouva.pdf' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Smazat dokument smlouva.pdf' })).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Soubor dokumentu')).not.toBeInTheDocument();
    });
});
