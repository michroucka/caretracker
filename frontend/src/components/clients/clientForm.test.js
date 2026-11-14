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
