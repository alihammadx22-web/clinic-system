import { writeFile } from "node:fs/promises";
import { resolve } from "node:path";

export default async function globalTeardown() {
  const stopFile = resolve(".next", "playwright-server.stop");
  await writeFile(stopFile, "stop", "utf8");

  // Give the wrapper time to stop Next before Playwright invokes its Windows
  // process-tree cleanup, which can otherwise remain open after the test report.
  await new Promise((resolveDelay) => setTimeout(resolveDelay, 750));
}
