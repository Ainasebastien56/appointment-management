# Appointment Management API

A RESTful API for managing medical appointments, built with Java, Spring Boot, Spring Data JPA, and PostgreSQL.

## Overview

The Appointment Management API provides endpoints to create, retrieve, update, delete, and filter medical appointments. It is designed as a backend project to practice REST API development, data persistence, request validation, exception handling, and automated testing.

## Tech Stack

- **Java 21**
- **Spring Boot 4.1.1**
- **Spring Web MVC** — REST endpoints
- **Spring Data JPA** — database access and persistence
- **PostgreSQL** — relational database
- **Jakarta Bean Validation** — request validation
- **Maven Wrapper** — project build and dependency management
- **JUnit and Spring Boot testing support** — automated tests

## Current Features

- Create a medical appointment
- Retrieve an appointment by ID
- Retrieve appointments using pagination
- Filter appointments by doctor name and status
- Update an existing appointment
- Delete an appointment
- Validate incoming requests
- Handle application errors through a global exception handler
- Test controller, service, and repository layers

## Getting Started

### Prerequisites

Make sure you have the following installed:

- JDK 21
- PostgreSQL
- Git

The project includes the Maven Wrapper, so a separate Maven installation is not required.

### 1. Clone the repository

```bash
git clone https://github.com/Ainasebastien56/appointment-management.git
cd appointment-management
```

### 2. Create the PostgreSQL database

Create a database named `appointment_management`.

For example, using `psql`:

```sql
CREATE DATABASE appointment_management;
```

### 3. Configure environment variables

The application reads its database connection settings from environment variables.

| Variable | Default / Example |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/appointment_management` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | Your local PostgreSQL password |

On Windows PowerShell, set the variables in your current terminal session:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/appointment_management"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-local-password"
```

Replace the example password with your own local database password. Never commit credentials to Git.

### 4. Run the application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API will normally be available at:

`http://localhost:8080`

### 5. Run the tests

On Windows:

```powershell
.\mvnw.cmd test
```

The project currently includes automated tests for the controller, service, and repository layers.

## API Endpoints

Base path: `/api/appointments`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/appointments` | List appointments with pagination and optional filters |
| `GET` | `/api/appointments/{id}` | Retrieve an appointment by ID |
| `POST` | `/api/appointments` | Create an appointment |
| `PUT` | `/api/appointments/{id}` | Update an existing appointment |
| `DELETE` | `/api/appointments/{id}` | Delete an appointment |

### Filtering and Pagination

The listing endpoint supports the optional `doctorName` and `status` query parameters, along with Spring Data pagination parameters.

Example:

```http
GET /api/appointments?doctorName=Dr%20Smith&status=SCHEDULED&page=0&size=10
```

Pagination parameters include `page`, `size`, and supported `sort` expressions.

### Example Request

Create an appointment using `POST /api/appointments`:

```json
{
  "patientName": "John Doe",
  "doctorName": "Dr. Smith",
  "appointmentDate": "2026-11-15",
  "status": "SCHEDULED"
}
```

The request requires non-blank patient and doctor names, a non-null appointment date, and a non-null status.

Available statuses in the current implementation:

- `SCHEDULED`
- `COMPLETED`
- `CANCELLED`

## Project Structure

```text
appointment-management/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/sebastien/appointmentmanagement/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       └── resources/
├── .mvn/
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Development Roadmap

Planned improvements include:

- Refactoring the domain model to introduce dedicated `Patient` and `Doctor` entities
- Implementing doctor availability and appointment scheduling rules
- Preventing conflicting bookings
- Adding authentication and role-based authorization
- Developing an Angular frontend
- Preparing the application for deployment

These items are planned improvements and are not all implemented in the current version.

## Project Status

The backend is an ongoing learning and portfolio project. Its current REST API has automated tests for the main application layers. Further development will focus on domain modeling, business rules, security, and frontend integration.

## License

No license has been added yet.
