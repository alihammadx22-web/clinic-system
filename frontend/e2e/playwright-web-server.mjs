import { existsSync, rmSync } from "node:fs";
import { resolve } from "node:path";
import { spawn } from "node:child_process";

const stopFile = resolve(".next", "playwright-server.stop");
const nextCli = resolve("node_modules", "next", "dist", "bin", "next");

rmSync(stopFile, { force: true });

const child = spawn(
  process.execPath,
  [nextCli, "start", "--hostname", "127.0.0.1", "--port", "3100"],
  {
    env: process.env,
    stdio: "inherit",
    windowsHide: true,
  },
);

let isStopping = false;

function stop() {
  if (isStopping) return;
  isStopping = true;
  clearInterval(stopPoll);
  child.kill();

  const forceExit = setTimeout(() => child.kill("SIGKILL"), 3_000);
  forceExit.unref();
}

const stopPoll = setInterval(() => {
  if (existsSync(stopFile)) stop();
}, 100);

child.once("exit", (code) => {
  clearInterval(stopPoll);
  rmSync(stopFile, { force: true });
  process.exit(code ?? 0);
});

process.once("SIGINT", stop);
process.once("SIGTERM", stop);
