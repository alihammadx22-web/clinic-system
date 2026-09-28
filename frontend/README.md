# Dental Clinic Management Frontend

A frontend-only Next.js 16, React 19, TypeScript, and Tailwind CSS demo for a generic dental clinic.

## Portals

- Reception: daily queue, patients, appointments, and doctors and schedules.
- Doctor: today’s worklist, patient history, and simple dental cases.
- Authentication: staff login for reception and doctor accounts.

The header role switcher changes between all three portals. The mock service persists shared data in browser `localStorage`; it is isolated under `src/features/dental` so a Spring Boot adapter can replace it later.

## Run and verify

```powershell
npm.cmd install
npm.cmd run dev
npm.cmd run lint
npm.cmd run typecheck
npm.cmd run build
```

Open `http://localhost:3000`. Any valid email and password of six or more characters can enter the demo. No backend or environment variables are required.
