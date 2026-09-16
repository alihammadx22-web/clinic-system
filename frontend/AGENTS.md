<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

## Clinic project context

Before changing clinic behavior, read `../HANDOFF.md`, `../physiotherapy_clinic_project_requirements_updated.md`, and `../clinic_development_roadmap_updated.md`. Verify the implementation and repository state instead of assuming documentation is exact.

Keep `src/app` focused on routing. Place business-facing UI, API wrappers, and
types under `src/features/<feature>`, third-party clients under
`src/integrations`, and only domain-neutral utilities under `src/shared`. Follow
`../ARCHITECTURE.md` and do not recreate a global all-domain API barrel.
