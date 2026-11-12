# Appointment Management

A full-stack application for managing medical appointments: a REST API built with Java 21, Spring Boot and PostgreSQL, and an Angular frontend.

Patients register, browse doctors' available slots and book appointments. Doctors manage the outcome and notes of their appointments. Administrators create doctor accounts, define doctor schedules and manage all appointments.

## Tech Stack

**Backend**

- Java 21, Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Jakarta Bean Validation
- Spring Security with OAuth2 Resource Server (JWT signed with HS256)
- PostgreSQL
- JUnit, Mockito and Spring Boot test support
- Maven Wrapper

**Frontend** (in [`frontend/`](frontend/))

- Angular 21 (standalone components, lazy-loaded routes)
- Tailwind CSS 4
- Vitest

## Features

- **Authentication**: patient self-registration and login. The API returns a JWT that must be sent as a `Bearer` token on every other request.
- **Roles**: `PATIENT`, `DOCTOR` and `ADMIN`. Registration always creates a patient account; users cannot choose their role. Doctor accounts are created by an administrator.
- **Doctor schedules**: weekly availability per doctor, plus date-specific exceptions (day off or custom hours).
- **Booking**: appointments are 30-minute slots aligned on the doctor's schedule. The API computes available slots and rejects conflicting bookings.
- **Appointment lifecycle**: statuses `SCHEDULED`, `COMPLETED`, `NO_SHOW` and `CANCELLED`. Every status change is recorded in a status history, with an optional reason.
- **Cancellation**: appointments are never deleted, only cancelled, so their history is kept. Patients cannot cancel less than 2 hours before the appointment starts.
- **Doctor notes** on appointments.
- **Patient statistics** for the patient dashboard.
- **Role-based dashboards** in the frontend for patients, doctors and administrators, with light and dark themes.

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL
- Node.js and npm (for the frontend)

The project includes the Maven Wrapper, so a separate Maven installation is not required.

### 1. Clone the repository

```bash
git clone https://github.com/Ainasebastien56/appointment-management.git
cd appointment-management
```

### 2. Create the databases

```sql
CREATE DATABASE appointment_management;
CREATE DATABASE appointment_management_test; -- used by the tests
```

The schema is created and updated automatically by Hibernate.

### 3. Configure environment variables

The backend reads its settings from environment variables, or from a `.env` file at the project root (this file is ignored by Git).

| Variable | Required | Description |
|---|---|---|
| `DB_USERNAME` | No (default `postgres`) | PostgreSQL user |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET_BASE64` | Yes | Base64-encoded key used to sign JWTs. It must decode to at least 32 bytes. |
| `BOOTSTRAP_ADMIN_EMAIL` | No | Email of the administrator account created at startup |
| `BOOTSTRAP_ADMIN_PASSWORD` | No | Password of that administrator account |

Example `.env` file:

```properties
DB_USERNAME=postgres
DB_PASSWORD=your-local-password
JWT_SECRET_BASE64=your-base64-secret
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=change-me
```

To generate a secret in PowerShell:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

If both bootstrap variables are set, an administrator account is created at startup when it does not already exist. This is how the first administrator is created.

Tokens expire after 60 minutes by default (`app.security.jwt.expiration-minutes`).

### 4. Run the backend

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, use `./mvnw` instead of `.\mvnw.cmd`. The API is available at `http://localhost:8080`.

### 5. Run the frontend

```powershell
cd frontend
npm install
npm start
```

The application is available at `http://localhost:4200`. This is the only origin allowed by the API's CORS configuration.

### 6. Run the tests

Backend (requires PostgreSQL and the `appointment_management_test` database):

```powershell
.\mvnw.cmd test
.\mvnw.cmd test -Dtest=AppointmentServiceTest   # a single test class
```

Frontend:

```powershell
cd frontend
npm test
```

## API Endpoints

All endpoints except `/api/auth/**` require an `Authorization: Bearer <token>` header. What an endpoint returns or allows depends on the role of the authenticated user.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create a patient account |
| `POST` | `/api/auth/login` | Log in and receive a JWT |

### Appointments

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| `GET` | `/api/appointments` | All | List appointments (paginated). Admins see all appointments, patients and doctors see their own. |
| `GET` | `/api/appointments/{id}` | All | Get one appointment the user has access to |
| `GET` | `/api/appointments/available-slots?date=&doctorId=` | Patient | List available 30-minute slots for a date |
| `GET` | `/api/appointments/my-statistics` | Patient | Statistics on the patient's appointments |
| `POST` | `/api/appointments` | Patient | Book an appointment |
| `PUT` | `/api/appointments/{id}` | Patient (own), Admin | Update an appointment |
| `PATCH` | `/api/appointments/{id}/status` | Doctor (own), Admin | Change the status, with an optional reason |
| `PATCH` | `/api/appointments/{id}/notes` | Doctor (own), Admin | Update the notes |
| `DELETE` | `/api/appointments/{id}` | Patient (own), Admin | Cancel an appointment (it is not deleted) |

`GET /api/appointments` accepts optional filters (`status`, `doctorName`, `doctorLastName`, `upcomingOnly`, `pendingOutcomeOnly`, `pastOutcomeOnly`) and Spring Data pagination parameters (`page`, `size`, `sort`).

Example booking request (`POST /api/appointments`):

```json
{
  "doctorId": 2,
  "appointmentDate": "2026-11-20",
  "startTime": "09:30",
  "type": "CONSULTATION",
  "reason": "Annual check-up"
}
```

`type` is `CONSULTATION` or `FOLLOW_UP`.

### Doctors

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| `GET` | `/api/doctors` | All | List doctors |

### Administration

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/doctors` | Create a doctor account and profile |
| `POST` | `/api/admin/doctors/{doctorId}/schedules` | Add a weekly availability (`dayOfWeek`, `startTime`, `endTime`) |
| `POST` | `/api/admin/doctors/{doctorId}/schedule-exceptions` | Add a schedule exception (`DAY_OFF` or `CUSTOM_HOURS`) for a date |
| `POST` | `/api/admin/appointments/{patientId}` | Book an appointment on behalf of a patient |

## Project Structure

```text
appointment-management/
├── src/main/java/com/sebastien/appointmentmanagement/
│   ├── config/       # security, JWT, CORS, admin bootstrap
│   ├── controller/   # REST controllers
│   ├── dto/          # request and response objects
│   ├── entity/       # JPA entities and enums
│   ├── exception/    # global exception handling
│   ├── repository/   # Spring Data repositories
│   └── service/      # business rules
├── src/test/         # controller, service and repository tests
├── frontend/         # Angular application
├── mvnw, mvnw.cmd
└── pom.xml
```

## Roadmap

- Let administrators create additional administrator accounts from the application
- Prepare the application for deployment

## License

No license has been added yet.
