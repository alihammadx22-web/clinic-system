# Backend Contract Draft

This document captures the frontend data model and API surface needed when replacing the current mock dental service with Spring Boot REST APIs. It is intentionally a draft; business entities and endpoints are not implemented yet.

## Base URL

- Local backend: `http://localhost:8080/api`
- Local frontend origin allowed by CORS: `http://localhost:3000` by default, configurable with `FRONTEND_ORIGIN`.

## Core Entities

### User and Auth

- `User`: `id`, `name`, `email`, `role`, `active`, `createdAt`
- Roles: `RECEPTION`, `DOCTOR`, `PATIENT`
- Auth screens currently need login, register, forgot password, and verification flows. The frontend is still demo-only, so token format and session handling can be finalized later.

### Patient

- `id`
- `name`
- `phone`
- `email`
- `dateOfBirth`
- `gender`: `Female`, `Male`, `Other`
- `address`
- `medicalNotes`
- `createdAt`

### Doctor

- `id`
- `name`
- `specialty`
- `phone`
- `email`
- `room`
- `fee`
- `workingDays`: array of numbers, where Sunday is `0` and Saturday is `6`
- `startTime`
- `endTime`
- `active`

### Appointment

- `id`
- `patientId`
- `doctorId`
- `date`
- `time`
- `duration`
- `reason`
- `notes`
- `status`: `Scheduled`, `Checked In`, `In Progress`, `Completed`, `Cancelled`
- `createdAt`

### Dental Case

- `id`
- `patientId`
- `doctorId`
- optional `appointmentId`
- `type`: `Filling`, `Extraction`, `Root Canal`, `Crown`, `Orthodontics`, `Cleaning`, `Whitening`, `Other`
- `notes`
- `status`: `Open`, `In Progress`, `Completed`
- `createdDate`
- optional `completedDate`

No tooth numbers, tooth charts, odontograms, tooth surfaces, or complex treatment plans are required.

### Schedule Exception

- `id`
- `doctorId`
- `date`
- `type`: `Absence`, `Late Arrival`, `Unavailable Hours`
- optional `startTime`
- optional `endTime`
- `note`

### Health

- `GET /health`
  - Returns backend status for local setup and frontend connectivity checks.

### Auth

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/forgot-password`
- `POST /auth/verify`
- `POST /auth/logout`
- `GET /auth/me`

### Patients

- `GET /patients`
- `GET /patients/{id}`
- `POST /patients`
- `PUT /patients/{id}`

Expected query support: search by name, phone, or email; filter by gender.

### Doctors and Schedules

- `GET /doctors`
- `GET /doctors/{id}`
- `PUT /doctors/{id}`
- `GET /doctors/{id}/schedule`
- `PUT /doctors/{id}/schedule`
- `GET /schedule/exceptions`
- `POST /schedule/exceptions`
- `PUT /schedule/exceptions/{id}`
- `DELETE /schedule/exceptions/{id}`

Expected query support: filter exceptions by doctor and date range.

### Appointments

- `GET /appointments`
- `GET /appointments/{id}`
- `POST /appointments`
- `PUT /appointments/{id}`
- `PATCH /appointments/{id}/status`
- `POST /appointments/{id}/check-in`
- `POST /appointments/{id}/cancel`
- `GET /appointments/available-slots`

Expected query support: filter by patient, doctor, date, status, and text search.

### Dental Cases

- `GET /dental-cases`
- `GET /dental-cases/{id}`
- `POST /dental-cases`
- `PUT /dental-cases/{id}`
- `PATCH /dental-cases/{id}/status`
- `POST /dental-cases/{id}/complete`

Expected query support: filter by patient, doctor, status, and case type.

## Response Shape Recommendation

Use plain JSON resources for single records and arrays for simple collections at first. If pagination becomes necessary, wrap collections in:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Validation errors should return field-level details:

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "must be a well-formed email address"
  }
}
```
