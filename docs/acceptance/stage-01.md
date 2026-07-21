# Stage 01 acceptance: project foundation

## 1. Stage objective

Establish a deployable and testable foundation for the TikTok Shop product selection system without implementing later-stage business capabilities. The stage provides the Next.js operations shell, Spring Boot API foundation, MySQL, Redis, Flyway, local object storage, Nginx, Docker Compose, CI, observability, and the operational health contract.

## 2. Completed work

- Initialized Next.js 15.5.20, React 19.1, TypeScript, Ant Design, TanStack Query, Zustand, React Hook Form, Zod, Vitest, and ECharts dependencies.
- Built a responsive operations status page for the foundation health endpoint.
- Initialized Spring Boot 3.5.7 on Java 21 with web, validation, security, JDBC, Redis, Actuator, MyBatis-Plus, Flyway, SpringDoc OpenAPI, JUnit, Mockito, Testcontainers, and JaCoCo dependencies.
- Added the common API response envelope, fixed error codes, global exception handling, request IDs, OpenAPI metadata, and public health endpoints.
- Added `GET /api/v1/health` with application, database, Redis, and storage checks.
- Added the `ObjectStorage` abstraction and path-safe `LocalObjectStorage` implementation.
- Configured MySQL 8.4, Redis 7.4, persistent volumes, local object storage, Nginx, Docker Compose, production Dockerfiles, graceful shutdown, health checks, and GitHub Actions CI.
- Added the required repository structure, environment template, Maven Wrapper, architecture notes, smoke test, and automated tests.

## 3. Incomplete work

None within Stage 01. Authentication, products, data imports, scoring, profit calculation, watchlists, alerts, and administration are deliberately excluded and remain blocked until their authorized stages.

## 4. Changed files

- Root and delivery: `.env.example`, `.gitignore`, `.dockerignore`, `docker-compose.yml`, `README.md`, `.github/workflows/ci.yml`.
- Backend: `backend/pom.xml`, Maven Wrapper files, Dockerfile, application configuration, 18 production Java source files, and 4 test classes.
- Frontend: package manifests, Next.js/TypeScript/Vitest/ESLint configuration, Dockerfile, application shell, health client, status component, styles, and component tests.
- Infrastructure: `deploy/nginx/nginx.conf`, `deploy/scripts/smoke-test.ps1`.
- Database: `database/migrations/README.md` and the empty seed directory marker.
- Documentation: `docs/architecture/stage-01-foundation.md` and this acceptance record.

## 5. Database migrations

- Flyway is enabled and scans `database/migrations` in local development and `/app/database/migrations` in the backend container.
- Stage 01 intentionally defines no business tables and therefore adds no versioned migration.
- `flyway_schema_history` is initialized successfully in MySQL. No executed migration file was modified.

## 6. API changes

- Added `GET /api/v1/health`.
- Added `GET /actuator/health` and `GET /actuator/info`.
- Added Swagger UI at `/swagger-ui.html` and OpenAPI JSON at `/v3/api-docs`.
- Added the common response envelope with `code`, `message`, `data`, `requestId`, and `timestamp`.
- The operational health endpoint returns HTTP 503 when any checked dependency is down.

Verified response:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "application": "UP",
    "database": "UP",
    "redis": "UP",
    "storage": "UP"
  }
}
```

The live response also includes a generated `requestId` and UTC `timestamp` as required by the global API contract.

## 7. Configuration changes

- Added non-secret environment placeholders for MySQL, Redis, JWT, initial administrator, data source encryption, local storage, analysis scheduling, and exposed ports.
- Added environment-driven Spring datasource, Redis, migration path, storage path, and server port configuration.
- Added UTC database configuration, Redis AOF persistence, Docker named volumes, isolated application networking, Nginx proxy routes, and service restart policies.
- Real `.env` files, build outputs, coverage results, local storage data, and temporary performance data are ignored by Git.

## 8. Test commands

```bash
docker compose up -d --build
docker compose ps
curl http://localhost/api/v1/health

cd backend
./mvnw -B -ntp clean verify

cd ../frontend
npm ci
npm run lint
npm run typecheck
npm run test
npm run test:coverage
npm run build
npm audit --omit=dev

powershell -ExecutionPolicy Bypass -File deploy/scripts/smoke-test.ps1
```

The final backend verification was also repeated in the pinned `maven:3.9.11-eclipse-temurin-21` image because the current host terminal exposes Java 17 rather than the required Java 21.

## 9. Actual test results

- Backend Maven verification: `BUILD SUCCESS`.
- Backend unit tests: 7 run, 0 failures, 0 errors, 0 skipped.
- Backend MVC integration tests: 1 run, 0 failures, 0 errors, 0 skipped.
- Frontend install: `npm ci` completed successfully.
- Frontend lint: passed with zero warnings.
- Frontend TypeScript check: passed.
- Frontend component tests: 2 run, 2 passed.
- Frontend production build: passed; the final build artifact was generated successfully.
- Production dependency audit: 0 vulnerabilities.
- Docker Compose validation: passed.
- Runtime status: MySQL, Redis, backend, and Nginx are healthy; the frontend container is running.
- Smoke test: passed.
- Browser checks: passed at desktop and 390 x 844 mobile viewports; refresh worked and the console contained no errors.

## 10. Coverage results

- Backend JaCoCo instruction coverage: 66.91% (453/677).
- Backend JaCoCo branch coverage: 50.00% (19/38).
- Backend JaCoCo line coverage: 64.83% (94/145).
- Frontend statement and line coverage: 45.89%.
- Frontend branch coverage: 71.42%.
- `foundation-status.tsx` line coverage: 100%.
- `health.ts` statement, branch, function, and line coverage: 100%.

Stage 01 has no mandated coverage threshold. Later-stage threshold requirements do not apply to this foundation stage.

## 11. Performance test results

No performance threshold is defined for Stage 01, so no formal load test was executed. Runtime smoke checks and interactive desktop/mobile checks completed successfully. Product-scale performance tests remain a Stage 03 and Stage 07 concern.

## 12. Acceptance criteria comparison

- [x] Next.js initialized.
- [x] Spring Boot initialized on Java 21.
- [x] MySQL configured.
- [x] Redis configured.
- [x] Flyway configured and validated at startup.
- [x] Swagger/OpenAPI configured.
- [x] Common response envelope configured.
- [x] Global exception handling configured.
- [x] Request ID propagation configured.
- [x] Actuator configured.
- [x] Docker Compose configured.
- [x] Nginx configured.
- [x] Local object storage abstraction and implementation configured.
- [x] CI configured.
- [x] Required directories created.
- [x] Health endpoint created and verified against the required dependency states.
- [x] Authentication, product, import, scoring, profit, watchlist, alert, and administration business logic not implemented.

## 13. Known issues

- Flyway 11.7.2 warns that MySQL 8.4 is newer than the MySQL 8.1 version it has tested. Validation, schema-history initialization, application startup, and health checks pass.
- MyBatis-Plus reports that no mapper is present. This is expected because Stage 01 contains no business persistence mapper.
- Mockito reports that dynamic Java-agent loading will be restricted in a future JDK release. Tests pass on Java 21; a later dependency/tooling update should adopt Mockito's recommended agent configuration.
- Development dependencies emit upstream deprecation notices. The production dependency audit reports zero vulnerabilities.
- The host terminal currently has Java 17 on its PATH. Host-side backend development requires Java 21 as documented; Docker-based Java 21 verification passes.

## 14. Git branch

`feature/stage-01-foundation`

## 15. Commit ID

Implementation commit: `8b1f5a08644573b29b438464dc5fbd3593da7936`

## 16. Push result

Success. The implementation commit was pushed to `origin/feature/stage-01-foundation`, and the local branch now tracks that remote branch.

## 17. Prerequisites for the next stage

- The user must explicitly reply `本阶段验收通过`.
- Only after that approval may this branch be merged into `main`, retested on `main`, pushed, and followed by creation of `feature/stage-02-auth`.
- Until approval, do not implement authentication or any later-stage business capability.
