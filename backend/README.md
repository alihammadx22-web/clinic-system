# Dental Clinic Backend

Spring Boot backend shell for the Dental Clinic Management System.

## Requirements

- Java 25
- PostgreSQL
- Maven Wrapper (`mvnw.cmd` on Windows, `./mvnw` on macOS/Linux)

## Local Configuration

Database configuration is read from environment variables only:

```powershell
$env:DENTAL_DB_URL="jdbc:postgresql://localhost:5432/dental_clinic"
$env:DENTAL_DB_USERNAME="postgres"
$env:DENTAL_DB_PASSWORD="postgres"
$env:JWT_SECRET="change-this-to-a-long-random-secret"
$env:FRONTEND_ORIGIN="http://localhost:3000"
```

Run the API:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```
