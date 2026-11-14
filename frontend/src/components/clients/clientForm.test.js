import { describe, expect, it } from "vitest";
import { emptyClient, toFormValues, toRequest, validateClient } from "./clientForm";

const personalNumberError = (personalNumber) =>
    validateClient({ ...emptyClient(1), personalNumber }).personalNumber;

describe("validateClient – personal number", () => {
    it.each(["", "010001", "999999"])("accepts %j, an empty number is given by the server", (personalNumber) => {
        expect(personalNumberError(personalNumber)).toBeUndefined();
    });

    it.each(["1", "12345", "1234567", "12a456"])("rejects %j, the variable symbol has room for exactly six digits", (personalNumber) => {
        expect(personalNumberError(personalNumber)).toBe("Osobní číslo musí mít 6 číslic");
    });
});

const filledClient = (overrides) => ({
    ...emptyClient(1),
    firstName: 'Marie', lastName: 'Nová', gender: 'FEMALE', dateOfBirth: '1940-03-15', caregiverId: 10,
    street: 'Americká 12', city: 'Plzeň', postalCode: '301 00',
    ...overrides,
});

describe('validateClient – accommodation', () => {
    it('requires a house for supported housing', () => {
        const errors = validateClient(filledClient({ accommodationType: 'SUPPORTED_HOUSING', supportedHousingId: null }));

        expect(errors.supportedHousingId).toBe('Vyberte dům s pečovatelskou službou');
    });

    it('does not check the address of supported housing, the server copies it from the house', () => {
        const errors = validateClient(filledClient({ accommodationType: 'SUPPORTED_HOUSING', supportedHousingId: 5, street: '', city: '', postalCode: '' }));

        expect(errors).toEqual({});
    });

    it('still requires the address for other types', () => {
        const errors = validateClient(filledClient({ accommodationType: 'RENTED_FLAT', street: '' }));

        expect(errors.street).toBe('Prosím zadejte ulici a číslo popisné');
    });

    it('rejects a negative travel distance', () => {
        expect(validateClient(filledClient({ travelDistanceKm: '-1' })).travelDistanceKm).toBe('Dojezdová vzdálenost nesmí být záporná');
    });

    it.each(['1.5', 'abc'])('rejects the travel distance %j that is not a whole number', (travelDistanceKm) => {
        expect(validateClient(filledClient({ travelDistanceKm })).travelDistanceKm).toBe('Zadejte dojezdovou vzdálenost v celých kilometrech');
    });

    it.each(['', '0', '12'])('accepts the travel distance %j', (travelDistanceKm) => {
        expect(validateClient(filledClient({ travelDistanceKm })).travelDistanceKm).toBeUndefined();
    });
});

describe('accommodation in the request', () => {
    it('sends the supported housing only for the supported housing type', () => {
        const supportedHousing = { id: 5, name: 'DPS Slovany' };
        const values = toFormValues({ accommodationType: 'SUPPORTED_HOUSING', supportedHousing });

        expect(values.supportedHousingId).toBe(5);
        expect(toRequest(values).supportedHousingId).toBe(5);
        expect(toRequest({ ...values, accommodationType: 'RENTED_FLAT' }).supportedHousingId).toBeNull();
    });

    it('turns empty accommodation fields into null and the distance into a number', () => {
        const request = toRequest(toFormValues(null));

        expect(request).toMatchObject({ accommodationType: null, travelDistanceKm: null, floorOrRoom: null, accessMethod: null, lockCode: null, accessNote: null, hasElevator: false });
        expect(toRequest({ ...toFormValues(null), travelDistanceKm: '12' }).travelDistanceKm).toBe(12);
    });
});

describe('validateClient – guardian', () => {
    it('requires the name and contact of the guardian of a client who is not legally competent', () => {
        const errors = validateClient(filledClient({ legallyCompetent: false, guardianName: ' ', guardianContact: '' }));

        expect(errors.guardianName).toBe('U nesvéprávného klienta vyplňte jméno a kontakt opatrovníka');
        expect(errors.guardianContact).toBe('U nesvéprávného klienta vyplňte jméno a kontakt opatrovníka');
    });

    it('accepts a guardian with both fields filled', () => {
        const errors = validateClient(filledClient({ legallyCompetent: false, guardianName: 'Jan Nový', guardianContact: '603 111 222' }));

        expect(errors).toEqual({});
    });

    it('does not ask a legally competent client for a guardian', () => {
        expect(validateClient(filledClient({ legallyCompetent: true }))).toEqual({});
    });
});

describe('validateClient – contract and care dates', () => {
    it('requires the contract end for a fixed-term contract', () => {
        const errors = validateClient(filledClient({ contractIndefinite: false, contractTo: '' }));

        expect(errors.contractTo).toBe('U smlouvy na dobu určitou vyplňte datum konce');
    });

    it('ignores a leftover contract end when the contract is indefinite', () => {
        const errors = validateClient(filledClient({ contractIndefinite: true, contractFrom: '2026-05-01', contractTo: '2026-01-01' }));

        expect(errors.contractTo).toBeUndefined();
    });

    it('rejects a contract that ends before it starts', () => {
        const errors = validateClient(filledClient({ contractIndefinite: false, contractFrom: '2026-05-01', contractTo: '2026-04-30' }));

        expect(errors.contractTo).toBe('Konec smlouvy nesmí být před jejím začátkem');
    });

    it('accepts a contract that ends the day it starts', () => {
        const errors = validateClient(filledClient({ contractIndefinite: false, contractFrom: '2026-05-01', contractTo: '2026-05-01' }));

        expect(errors).toEqual({});
    });

    it('rejects care that ends before it starts', () => {
        const errors = validateClient(filledClient({ careFrom: '2026-05-01', careTo: '2026-04-30' }));

        expect(errors.careTo).toBe('Konec péče nesmí být před jejím začátkem');
    });

    it.each([
        { careFrom: '', careTo: '2026-04-30' },
        { careFrom: '2026-05-01', careTo: '' },
    ])('does not compare care dates when one is missing: %j', (dates) => {
        expect(validateClient(filledClient(dates)).careTo).toBeUndefined();
    });
});

describe('validateClient – second phone', () => {
    it('checks the format like the first phone', () => {
        expect(validateClient(filledClient({ phone2: '12345' })).phone2).toBe('Telefon ve tvaru 123 456 789 nebo +420 123 456 789');
        expect(validateClient(filledClient({ phone2: '603 111 222' })).phone2).toBeUndefined();
    });
});

describe('additional details in the request', () => {
    it('always sends every new field, because saving replaces the whole client', () => {
        const request = toRequest(toFormValues(null));

        expect(request).toMatchObject({
            titleBefore: null, titleAfter: null, phone2: null,
            guardianName: null, guardianContact: null,
            careFrom: null, careTo: null,
            contractFrom: null, contractTo: null, contractIndefinite: true,
            noCharge: false, intakeAssessmentDate: null,
        });
    });

    it('sends the stored values of an existing client unchanged', () => {
        const client = {
            titleBefore: 'Mgr.', titleAfter: 'DiS.', phone2: '603111222',
            legallyCompetent: false, guardianName: 'Jan Nový', guardianContact: 'jan@example.cz',
            careFrom: '2026-01-01', careTo: '2026-12-31',
            contractFrom: '2026-01-01', contractTo: '2026-12-31', contractIndefinite: false,
            noCharge: true, intakeAssessmentDate: '2025-12-15',
        };

        expect(toRequest(toFormValues(client))).toMatchObject({ ...client, phone2: '603 111 222' });
    });

    it('does not send a contract end of an indefinite contract', () => {
        const values = { ...toFormValues({ contractTo: '2026-12-31' }), contractIndefinite: true };

        expect(toRequest(values).contractTo).toBeNull();
    });
});
