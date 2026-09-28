import { expect, test } from "@playwright/test";

test("login page is staff-only", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByText("Secure staff access")).toBeVisible();
  await expect(page.getByText("Sign in with a reception or doctor account.")).toBeVisible();
  await expect(page.getByText("Register")).toHaveCount(0);
});
