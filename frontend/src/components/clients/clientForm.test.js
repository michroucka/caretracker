import { describe, expect, it } from "vitest";
import { emptyClient, validateClient } from "./clientForm";

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
