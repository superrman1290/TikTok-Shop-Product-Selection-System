# Stage 01 architecture

## Runtime topology

```text
Browser
  -> Nginx :80
      -> Next.js :3000
      -> Spring Boot :8080
          -> MySQL :3306
          -> Redis :6379
          -> Local object storage volume
```

The MVP starts as a modular monolith. The backend owns domain behavior and exposes REST APIs under `/api/v1`; the frontend renders backend results and does not calculate business scores.

## Foundation boundaries

Stage 01 contains only shared HTTP infrastructure, health reporting, configuration, storage abstraction, deployment configuration, and test scaffolding. Authentication, product data, imports, analysis, profit calculation, watchlists, alerts, and administration remain outside this stage.

## Health contract

`GET /api/v1/health` returns the common API envelope and reports application, database, Redis, and storage states. The endpoint returns HTTP 503 if any dependency is down. Actuator exposes only `health` and `info`.

## Configuration

Runtime credentials are supplied through environment variables. `.env.example` contains non-secret placeholders, while `.env` is ignored. Local object storage generates object keys and never uses a client filename as a disk path.
