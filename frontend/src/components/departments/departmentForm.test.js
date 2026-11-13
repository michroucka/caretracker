import { describe, expect, it } from "vitest";
import { coordinatorOptions, EMPTY_DEPARTMENT, formatAddress, toFormValues, toRequest, validateDepartment } from "./departmentForm";

const validDepartment = { ...EMPTY_DEPARTMENT, street: "Hlavní 1", city: "Plzeň", departmentNumber: "3" };

describe("toFormValues", () => {
    it("matches the empty form for a new department", () => {
        expect(toFormValues(null)).toEqual(EMPTY_DEPARTMENT);
    });

    it("turns the department number into text and takes the coordinator id", () => {
        const values = toFormValues({ street: "Hlavní 1", city: "Plzeň", postalCode: "301 00", departmentNumber: 0, coordinator: { id: 4 } });

        expect(values).toEqual({ street: "Hlavní 1", city: "Plzeň", postalCode: "301 00", departmentNumber: "0", coordinatorId: 4 });
    });
});

describe("validateDepartment", () => {
    it("accepts a street, a city and a department number", () => {
        expect(validateDepartment(validDepartment)).toEqual({});
    });

    it.each(["", "   "])("requires the street and city (%j)", (blank) => {
        expect(validateDepartment({ ...validDepartment, street: blank, city: blank })).toEqual({
            street: "Prosím zadejte ulici",
            city: "Prosím zadejte město",
        });
    });

    it("requires the department number", () => {
        expect(validateDepartment({ ...validDepartment, departmentNumber: "" }).departmentNumber).toBe("Číslo střediska je povinné");
    });

    it.each(["1", "99"])("accepts the department number %j", (departmentNumber) => {
        expect(validateDepartment({ ...validDepartment, departmentNumber }).departmentNumber).toBeUndefined();
    });

    it.each(["0", "100"])("rejects the department number %j", (departmentNumber) => {
        expect(validateDepartment({ ...validDepartment, departmentNumber }).departmentNumber).toBe("Číslo střediska musí být mezi 1 a 99");
    });

    it.each(["", "30100", "301 00"])("accepts the postal code %j", (postalCode) => {
        expect(validateDepartment({ ...validDepartment, postalCode }).postalCode).toBeUndefined();
    });

    it.each(["3010", "301 000"])("rejects the postal code %j", (postalCode) => {
        expect(validateDepartment({ ...validDepartment, postalCode }).postalCode).toBe("PSČ má 5 číslic");
    });
});

describe("toRequest", () => {
    it("trims texts and sends empty optional fields as null", () => {
        expect(toRequest({ ...validDepartment, street: "  Hlavní 1 ", city: " Plzeň " })).toEqual({
            street: "Hlavní 1",
            city: "Plzeň",
            postalCode: null,
            departmentNumber: 3,
            coordinatorId: null,
            organizationId: null,
        });
    });

    it("sends the department number as a number and the chosen organization", () => {
        const request = toRequest({ ...validDepartment, postalCode: "301 00", departmentNumber: "12", coordinatorId: 4 }, 2);

        expect(request).toMatchObject({ postalCode: "301 00", departmentNumber: 12, coordinatorId: 4, organizationId: 2 });
    });
});

describe("coordinatorOptions", () => {
    const employees = [
        { id: 1, firstName: "Jana", lastName: "Nová", role: "CAREGIVER", department: { city: "Plzeň" } },
        { id: 2, firstName: "Petr", lastName: "Starý", role: "COORDINATOR", department: { city: "Cheb" } },
        { id: 3, firstName: "Eva", lastName: "Malá", role: "COORDINATOR" },
    ];

    it("leaves out coordinators of other departments", () => {
        expect(coordinatorOptions(employees, null).map((option) => option.value)).toEqual([1]);
    });

    it("keeps the department's own coordinator", () => {
        expect(coordinatorOptions(employees, 2).map((option) => option.value)).toEqual([1, 2]);
    });

    it("labels options with the full name and the employee's city", () => {
        expect(coordinatorOptions(employees, null)[0]).toEqual({ value: 1, label: "Jana Nová", description: "Plzeň" });
    });
});

describe("formatAddress", () => {
    it.each([
        [{ street: "Hlavní 1", postalCode: "301 00", city: "Plzeň" }, "Hlavní 1, 301 00 Plzeň"],
        [{ street: "Hlavní 1", city: "Plzeň" }, "Hlavní 1, Plzeň"],
        [{ city: "Plzeň" }, "Plzeň"],
        [{ street: "Hlavní 1" }, "Hlavní 1"],
        [{}, ""],
    ])("%j → %j", (department, expected) => {
        expect(formatAddress(department)).toBe(expected);
    });
});
