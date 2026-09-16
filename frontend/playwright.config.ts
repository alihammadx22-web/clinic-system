import { defineConfig, devices } from "@playwright/test";
import { loadEnvConfig } from "@next/env";

loadEnvConfig(process.cwd());

// Playwright's automatic failure DOM snapshot can include filled credentials.
process.env.PLAYWRIGHT_NO_COPY_PROMPT = "1";

const baseURL = process.env.QA_BASE_URL ?? "http://127.0.0.1:3100";
if (!["127.0.0.1", "localhost"].includes(new URL(baseURL).hostname)) {
  throw new Error("Browser QA is restricted to a local synthetic clinic instance.");
}

export default defineConfig({
  testDir: "./e2e",
  globalTeardown: "./e2e/global-teardown.ts",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  reporter: "list",
  use: {
    baseURL,
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
    timezoneId: "America/New_York",
    trace: "off",
    screenshot: "off",
    video: "off",
  },
  projects: [
    { name: "chromium", testMatch: "demo.spec.ts", use: { ...devices["Desktop Chrome"], channel: "chrome" } },
  ],
  webServer: {
    command: "node e2e/playwright-web-server.mjs",
    url: baseURL,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
