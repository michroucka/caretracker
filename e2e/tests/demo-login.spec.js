import { expect, test } from "./support/test.js";

test("the demo login page says the people are made up and the photos AI generated", async ({ page }) => {
    await page.goto("/login");

    await expect(page.getByText("Demo obsahuje smyšlené osoby, fotografie jsou vygenerované AI.")).toBeVisible();
    await expect(page.getByRole("button", { name: /coordinator/ })).toBeVisible();
});
