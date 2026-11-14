import { describe, expect, it } from "vitest";
import { EMPTY_TASK, formatTaskPrice, statutoryPrefill, toFormValues, toRequest, unitTypeLabel, validateTask } from "./taskForm";

const validTask = { ...EMPTY_TASK, name: "Nákup", unitPrice: "120" };
const normalizeSpaces = (text) => text.replace(/\s/g, " ");

describe("unitTypeLabel", () => {
    it.each([
        ["HOUR", "Hodina"],
        ["OCCURRENCE", "Úkon"],
        ["KM", "Kilometr"],
        ["KG", "Kilogram"],
    ])("%s → %s", (unitType, label) => {
        expect(unitTypeLabel(unitType)).toBe(label);
    });

    it("passes an unknown unit through", () => {
        expect(unitTypeLabel("LITER")).toBe("LITER");
    });
});

describe("formatTaskPrice", () => {
    it("formats the price with thousands separators and the unit", () => {
        expect(normalizeSpaces(formatTaskPrice({ unitPrice: 1200, unitType: "HOUR" }))).toBe("1 200 Kč / hod");
    });

    it("falls back to plain Kč for an unknown unit", () => {
        expect(formatTaskPrice({ unitPrice: 50, unitType: "LITER" })).toBe("50 Kč");
    });
});

describe("toFormValues", () => {
    it("matches the empty form for a new task", () => {
        expect(toFormValues(null)).toEqual(EMPTY_TASK);
    });

    it("turns the price into text", () => {
        expect(toFormValues({ name: "Nákup", unitType: "KM", unitPrice: 8, doubleMeeting: true })).toEqual({
            name: "Nákup", unitType: "KM", unitPrice: "8", doubleMeeting: true, statutoryTaskId: null,
        });
    });

    it("keeps the link to the statutory item", () => {
        expect(toFormValues({ name: "Nákup", unitType: "KM", unitPrice: 8, statutoryTaskId: 3 }).statutoryTaskId).toBe(3);
    });
});

describe("validateTask", () => {
    it("accepts a named task with a positive price", () => {
        expect(validateTask(validTask)).toEqual({});
    });

    it("requires the name and price", () => {
        expect(validateTask({ ...EMPTY_TASK, name: "   " })).toEqual({ name: "Prosím zadejte název", unitPrice: "Prosím zadejte cenu" });
    });

    // the backend rejects unitPrice <= 0 as well (TaskService)
    it.each(["0", "-5"])("rejects the price %j", (unitPrice) => {
        expect(validateTask({ ...validTask, unitPrice }).unitPrice).toBe("Cena musí být větší než 0");
    });
});

describe("toRequest", () => {
    it("trims the name, sends the price as a number and the chosen organization", () => {
        expect(toRequest({ ...validTask, name: " Nákup " }, 4)).toEqual({
            name: "Nákup", unitType: "HOUR", unitPrice: 120, doubleMeeting: false, statutoryTaskId: null, organizationId: 4,
        });
    });

    it("sends the chosen statutory item", () => {
        expect(toRequest({ ...validTask, statutoryTaskId: 3 }).statutoryTaskId).toBe(3);
    });

    it("sends a missing organization as null", () => {
        expect(toRequest(validTask).organizationId).toBeNull();
    });
});

describe("validateTask against a statutory item", () => {
    const walk = { id: 3, maxUnitPrice: 100 };

    it("accepts a price up to the maximum", () => {
        expect(validateTask({ ...validTask, statutoryTaskId: 3, unitPrice: "100" }, [walk])).toEqual({});
    });

    it("rejects a price over the maximum with the server's message", () => {
        expect(validateTask({ ...validTask, statutoryTaskId: 3, unitPrice: "101" }, [walk]).unitPrice)
            .toBe("Cena převyšuje maximální úhradu podle vyhlášky (100 Kč)");
    });

    it("does not limit a task without a link", () => {
        expect(validateTask({ ...validTask, unitPrice: "5000" }, [walk])).toEqual({});
    });
});

describe("statutoryPrefill", () => {
    it("takes the name, unit and maximum price of the item", () => {
        expect(statutoryPrefill({ id: 3, shortName: "Procházka", unitType: "HOUR", maxUnitPrice: 100 })).toEqual({
            statutoryTaskId: 3, name: "Procházka", unitType: "HOUR", unitPrice: "100",
        });
    });

    it("only clears the link when no item is chosen", () => {
        expect(statutoryPrefill(null)).toEqual({ statutoryTaskId: null });
    });
});
