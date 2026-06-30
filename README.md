# Forum MSE 2026

A Spring Boot forum application with PostgreSQL, JWT authentication, Flyway database migrations, OpenAPI documentation, and a bundled browser UI.

## Features

- Topic and reply browsing
- User registration and login
- JWT-based authentication
- Admin user management
- Moderator role support
- PostgreSQL persistence
- Flyway schema migrations
- Actuator health endpoints
- Scalar/OpenAPI documentation
- Docker Compose setup for the app, database, and optional Adminer UI

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- PostgreSQL 16
- Flyway
- Maven
- Docker Compose

## Quick Start With Docker

Create a local environment file:

```powershell
Copy-Item .env.dev.example .env
```

Edit `.env` and set real values. For local Docker Compose, the important values are:

```env
SPRING_PROFILES_ACTIVE=dev

POSTGRES_DB=forum
POSTGRES_USER=admin
POSTGRES_PASSWORD=change-me-dev-db-password

JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION_MS=86400000
```

Run the full stack:

```powershell
docker compose up --build
```

Or run it in the background:

```powershell
docker compose up --build -d
```

Open the app:

```text
http://localhost:9000
```

Default administrator:

```text
username: admin
password: admin
```

## Services

| Service | URL | Description |
| --- | --- | --- |
| App | `http://localhost:9000` | Forum UI and REST API |
| API Docs | `http://localhost:9000/docs` | Scalar/OpenAPI documentation |
| Health | `http://localhost:9000/actuator/health` | Spring Boot health endpoint |
| Liveness | `http://localhost:9000/livez` | Liveness probe |
| Readiness | `http://localhost:9000/readyz` | Readiness probe |
| Adminer | `http://localhost:8090` | Optional database UI |

Start Adminer with:

```powershell
docker compose up -d adminer
```

Adminer connection values:

```text
System: PostgreSQL
Server: postgres
Username: value of POSTGRES_USER
Password: value of POSTGRES_PASSWORD
Database: value of POSTGRES_DB
```

## Docker Commands

Build the app image:

```powershell
docker compose build app
```

Start everything:

```powershell
docker compose up -d
```

View app logs:

```powershell
docker compose logs -f app
```

Stop containers:

```powershell
docker compose down
```

Stop containers and delete the local PostgreSQL volume:

```powershell
docker compose down -v
```

## Local Development Without Dockerizing The App

Prerequisites:

- JDK 21
- Docker Desktop
- Maven wrapper from this repository

Start only PostgreSQL:

```powershell
docker compose up -d postgres
```

Load `.env` into the current PowerShell session:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
    }
}
```

If you run the app outside Docker, make sure `SPRING_DATASOURCE_URL` points to localhost:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/forum
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=change-me-dev-db-password
```

Run the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

## Configuration

The app uses Spring profiles:

- `dev`
- `test`
- `uat`
- `prod`

Configuration files live in:

```text
src/main/resources/application*.yaml
```

Secrets are provided with environment variables, not committed YAML files.

Important variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile |
| `POSTGRES_DB` | PostgreSQL database name |
| `POSTGRES_USER` | PostgreSQL user |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `SPRING_DATASOURCE_URL` | JDBC URL when running outside Docker |
| `SPRING_DATASOURCE_USERNAME` | Database username when running outside Docker |
| `SPRING_DATASOURCE_PASSWORD` | Database password when running outside Docker |
| `JWT_SECRET` | JWT signing secret, at least 32 characters |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds |

## Database

PostgreSQL runs in Docker using the `postgres:16-alpine` image.

Flyway automatically applies migrations from:

```text
src/main/resources/db/migration
```

The Docker Compose setup uses a named volume:

```text
postgres-data
```

This keeps local database data across container restarts.

## API Documentation

The OpenAPI specification is stored at:

```text
src/main/resources/openapi/openapi.yaml
```

When the app is running, open:

```text
http://localhost:9000/docs
```

The raw OpenAPI JSON is available at:

```text
http://localhost:9000/v3/api-docs
```

## Testing

Run tests with:

```powershell
.\mvnw.cmd test
```

Some tests use Testcontainers, so Docker must be running and available to the shell.

To only check compilation and packaging:

```powershell
.\mvnw.cmd -DskipTests package
```

## Notes

- The default admin user is created by Flyway migrations.
- The default admin password is `admin`; change it before using this outside local development.
- Regular self-registration always creates a `USER` account.
- Admins can promote users to `MODERATOR`, but not create or promote another admin through the UI/API.
