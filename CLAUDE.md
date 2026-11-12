# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Medical appointment management app: a Spring Boot 4.1 / Java 21 REST API (repo root) backed by PostgreSQL, plus an Angular 21 SPA in [frontend/](frontend/). Code comments and log messages are mostly in French; API error messages are in English.

[README.md](README.md) documents setup and the full endpoint list; keep it in sync when endpoints change.

## Commands

Backend (run from repo root; Windows uses `.\mvnw.cmd`, otherwise `./mvnw`):

```powershell
.\mvnw.cmd spring-boot:run                       # API on http://localhost:8080
.\mvnw.cmd test                                  # all tests
.\mvnw.cmd test -Dtest=AppointmentServiceTest    # one test class
.\mvnw.cmd test -Dtest=AppointmentServiceTest#methodName   # one test method
.\mvnw.cmd package
```

Frontend (run from `frontend/`):

```powershell
npm start          # ng serve on http://localhost:4200 (the only CORS origin the API allows)
npm run build
npm test           # ng test, using Vitest + jsdom
```

Prettier is configured in [frontend/.prettierrc](frontend/.prettierrc). There is no backend linter.

## Configuration

`application.properties` imports an optional `.env` file from the working directory (gitignored). Required/used variables:

- `DB_PASSWORD` (required), `DB_USERNAME` (default `postgres`). The datasource URL is hardcoded to `localhost:5432/appointment_management`.
- `JWT_SECRET_BASE64` (required): a Base64 HS256 key that decodes to at least 32 bytes, or startup fails.
- `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` (optional): [AdminBootstrapRunner](src/main/java/com/sebastien/appointmentmanagement/config/AdminBootstrapRunner.java) creates an ADMIN user at startup if they are set. This is the only way to create an admin account.
- `app.security.jwt.expiration-minutes` (default 60).

Schema is managed by Hibernate `ddl-auto=update`. There are no migrations.

**Tests need a running PostgreSQL** with a database named `appointment_management_test` ([src/test/resources/application.properties](src/test/resources/application.properties) uses `create-drop` and a fixed test JWT key). `AppointmentRepositoryTest` (`@DataJpaTest` with `Replace.NONE`) and the `@SpringBootTest` context test hit this real database. Controller tests (`@WebMvcTest` + `@Import(SecurityConfig.class)` + `@WithMockUser`, with `@MockitoBean` services) and service tests (Mockito) do not.

## Backend architecture

Package `com.sebastien.appointmentmanagement`: the usual `controller` → `service` → `repository` → `entity` layers, with request/response `dto` classes (entities are never returned directly).

**Auth model.** [SecurityConfig](src/main/java/com/sebastien/appointmentmanagement/config/SecurityConfig.java) sets up stateless OAuth2 Resource Server JWT validation with a symmetric HS256 key. The same `SecretKey` bean is used by the `JwtEncoder` that [JwtService](src/main/java/com/sebastien/appointmentmanagement/service/JwtService.java) uses to issue tokens. The token's `sub` is the user's email and a custom `role` claim holds `PATIENT`/`DOCTOR`/`ADMIN`. Only `/api/auth/**` is public.

**Authorization is done manually, not with Spring authorities.** There is no `@PreAuthorize` and no role mapping from the JWT. Controllers call `currentUserService.requireUser(authentication.getName())` to load the `User` from the DB, then branch on `user.getRole()`. They resolve the matching profile with `requirePatientProfile` / `requireDoctorProfile` ([CurrentUserService](src/main/java/com/sebastien/appointmentmanagement/service/CurrentUserService.java)) and throw `ResponseStatusException(FORBIDDEN)` for disallowed roles. Some admin checks live in the service instead (for example, `DoctorService.createDoctor` receives the caller's email). Follow this pattern for new endpoints, and always derive patient/doctor IDs from the authenticated profile, never from request input.

**Domain.** `User` (credentials + role) has a one-to-one link to either a `Patient` or a `Doctor` profile. `DoctorSchedule` (weekly availability) and `DoctorScheduleException` (date overrides) drive slot generation. Appointments are fixed **30-minute slots aligned to schedule boundaries**. [AppointmentService](src/main/java/com/sebastien/appointmentmanagement/service/AppointmentService.java) holds most business rules: conflict detection (409 CONFLICT), available-slot calculation, status transitions (`SCHEDULED`, `COMPLETED`, `NO_SHOW`, `CANCELLED`) recorded in `AppointmentStatusHistory`, doctor notes, patient statistics, and a rule that patients can't cancel less than 2 hours before the appointment starts. Appointments are never deleted, by design, so their history is kept: `DELETE /api/appointments/{id}` sets the status to `CANCELLED`. Doctors cancel instead through `PATCH /{id}/status` with `CANCELLED`, with a required reason, and only before the start time. Every cancellation writes a status history row. The reason is stored on `Appointment.cancellationReason` and shown to the patient, while `notes` stay hidden from patients on cancelled appointments (`hideNotesForPatient`). Don't add code that physically removes appointment rows.

**Endpoints.** `/api/auth` (login, register). Registration always creates a PATIENT by design; users must never be able to choose their role. DOCTOR accounts (and, later, extra ADMIN accounts) are created only by an admin. `/api/appointments` is role-aware: the same `GET` returns all appointments for admins, the patient's own for patients, and the doctor's own for doctors. The accepted filters differ by role: the doctor branch takes the temporal flags plus `date`, and gets a default sort. Statistics have one endpoint per role: `/my-statistics` for patients and `/my-doctor-statistics` for doctors. `/api/doctors` lists doctors. `/api/admin/...` covers creating doctors, their schedules and schedule exceptions, and booking for a patient.

Errors: [GlobalExceptionHandler](src/main/java/com/sebastien/appointmentmanagement/exception/GlobalExceptionHandler.java) maps exceptions to `ErrorResponse`. Business errors are mostly raised as `ResponseStatusException`.

## Frontend architecture

Angular 21 with standalone components, lazy-loaded routes ([app.routes.ts](frontend/src/app/app.routes.ts)), Tailwind CSS 4 (via PostCSS), and Vitest for tests.

- Services hardcode `http://localhost:8080/api/...` as the base URL (there are no environment files).
- After login the JWT is stored in `localStorage` or `sessionStorage` under `access_token`. [auth-interceptor](frontend/src/app/interceptors/auth-interceptor.ts) attaches it as a Bearer token to every request except `/api/auth/` ones.
- [auth-guard](frontend/src/app/guards/auth-guard.ts) decodes the token client-side (`AuthService.getRoleFromToken`, which also checks `exp`) and compares it to the route's `data.role`. A wrong role is redirected to that role's dashboard.
- Patient pages (`patient-dashboard`, `book-appointment`, `my-appointments`) are children of the shared `PatientLayout`. Doctor and admin dashboards are standalone routes.
- [theme.service](frontend/src/app/services/theme.service.ts) and `theme-toggle` provide light/dark theming.
