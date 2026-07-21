# TikTok Shop Product Selection System

Stage 01 establishes the deployable foundation for the TikTok Shop product selection system. It includes the Next.js operations shell, Spring Boot API foundation, MySQL, Redis, local object storage, Nginx, Docker Compose, Flyway, OpenAPI, health checks, and CI.

## Requirements

- Docker Desktop with Docker Compose
- Java 21 for host-side backend development
- Node.js 22 or later and npm for host-side frontend development

## Start the stack

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
curl http://localhost/api/v1/health
```

The operations console is available at `http://localhost`, Swagger UI at `http://localhost/swagger-ui.html`, and Actuator health at `http://localhost/actuator/health`.

## Verify the backend

```bash
cd backend
./mvnw clean verify
```

## Verify the frontend

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm run test
npm run build
```

## Repository layout

```text
frontend/    Next.js application and tests
backend/     Spring Boot application and tests
database/    Immutable Flyway migrations and seed assets
tools/       Repeatable data tooling introduced by later stages
deploy/      Nginx and operational scripts
docs/        Architecture, API, database, algorithm, and acceptance records
tmp/         Ignored local performance artifacts
```

The project is delivered in seven independently accepted stages. A stage branch is not merged into `main` until its acceptance is explicitly confirmed.
