import { expect, test } from "@playwright/test";

test("switches between all three demo portals", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email address").fill("demo@example.com");
  await page.getByLabel("Password").fill("demo123");
  await page.getByRole("button", { name: "Sign in to demo" }).click();
  await expect(page).toHaveURL(/\/reception$/);
  await expect(page.getByRole("heading", { name: "Good morning, Reception" })).toBeVisible();
  await page.getByRole("button", { name: "doctor", exact: true }).click();
  await expect(page).toHaveURL(/\/doctor$/);
  await page.getByRole("button", { name: "patient", exact: true }).click();
  await expect(page).toHaveURL(/\/patient$/);
});

test("reception can add and search for a patient", async ({ page }) => {
  await page.goto("/reception/patients");
  await page.getByRole("button", { name: "Add patient" }).first().click();
  await page.getByLabel("Full name *").fill("Demo Dental Patient");
  await page.getByLabel("Phone *").fill("+1 555 909 2222");
  await page.getByLabel("Date of birth *").fill("1990-05-20");
  await page.getByRole("button", { name: "Add patient" }).last().click();
  await page.getByPlaceholder("Search name, phone, or email…").fill("Demo Dental");
  await expect(page.getByText("Demo Dental Patient")).toBeVisible();
});

test("patient books an available appointment", async ({ page }) => {
  await page.goto("/patient/book");
  await page.getByRole("button", { name: /Dr. Sarah Mitchell/ }).click();
  await page.getByRole("button", { name: "Continue" }).click();
  const date = new Date();
  do { date.setDate(date.getDate() + 1); } while (![1, 2, 3, 4, 5].includes(date.getDay()));
  const dateValue = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  await page.getByLabel("Appointment date").fill(dateValue);
  await page.getByRole("button", { name: "Continue" }).click();
  await page.locator("button").filter({ hasText: /^\d{2}:\d{2}$/ }).first().click();
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByLabel("Reason for visit *").fill("Routine dental examination");
  await page.getByRole("button", { name: "Confirm appointment" }).click();
  await expect(page.getByRole("heading", { name: "You’re all set" })).toBeVisible();
});

test("doctor creates a case and reception records a payment", async ({ page }) => {
  await page.goto("/doctor/cases");
  await page.getByRole("button", { name: "New case" }).click();
  await page.getByLabel("Clinical notes *").fill("Cleaning review completed with healthy tissue response.");
  await page.getByRole("button", { name: "Create case" }).click();
  await expect(page.getByText("Dental case created")).toBeVisible();
  await page.goto("/reception/payments");
  await page.getByRole("button", { name: "Record payment" }).first().click();
  await page.getByRole("button", { name: "Record card payment" }).click();
  await expect(page.getByText("Payment recorded successfully")).toBeVisible();
});
