# TikTok Shop Product Selection System

The repository currently includes the deployable foundation, authentication and authorization, and Stage 03 product-data workflow. Administrators can import product and daily-stat CSV files, inspect row-level errors, and maintain product basics. Authenticated users can browse a server-paginated product ranking, open product details, and inspect daily price and sales trends.

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
docker run --rm \
  --network tiktok-product-insight_app \
  -e TEST_MYSQL_HOST=mysql \
  -e TEST_MYSQL_PASSWORD=change_me \
  -v tiktok_stage03_maven_cache:/root/.m2 \
  -v "$PWD:/workspace" \
  -w /workspace/backend \
  maven:3.9.11-eclipse-temurin-21 \
  mvn -B -ntp clean verify
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

## Generate and import product data

```bash
node tools/generate-mock-data.mjs \
  --products 120 \
  --seed 20260721 \
  --profile functional \
  --output tmp/functional-data

powershell -ExecutionPolicy Bypass -File deploy/scripts/stage-03-smoke-test.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/stage-03-performance-test.ps1
```

CSV imports accept UTF-8 files up to 20 MB and 200,000 rows. Product writes are idempotent by `platform + market + external_product_id`; daily statistics are idempotent by `product_id + stat_date`.

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
