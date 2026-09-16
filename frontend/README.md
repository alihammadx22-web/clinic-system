# Dental Clinic Management Frontend

A frontend-only Next.js 16, React 19, TypeScript, and Tailwind CSS demo for a generic dental clinic.

## Portals

- Reception: daily queue, patients, appointments, doctors and schedules, payments, and clinic settings.
- Doctor: today’s worklist, patient history, and simple dental cases.
- Patient: dashboard, guided booking, appointments, dental cases, and profile.
- Authentication: login, registration, password reset, and verification demo screens.

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
