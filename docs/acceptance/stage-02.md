# Stage 02 acceptance: authentication and authorization

## 1. Stage objective

Deliver secure registration, login, token refresh and rotation, logout, current-user lookup, password changes, role authorization, account enable/disable enforcement, login failure controls, initial administrator provisioning, authentication pages, and protected frontend routes without implementing later-stage product or analysis features.

## 2. Completed work

- Added layered authentication API, application, domain, DTO, and infrastructure packages.
- Added BCrypt password hashing at strength 12 and the required 8-to-64-character letter/digit policy.
- Added two-hour HS256 Access Tokens with unique JWT IDs and current-account validation on every authenticated request.
- Added 30-day opaque Refresh Tokens, HMAC-SHA256 database hashes, HttpOnly SameSite Strict cookies, transactional rotation, replay rejection, logout revocation, and global revocation after password changes or account disablement.
- Added `USER` and `ADMIN` roles, `ENABLED` and `DISABLED` statuses, administrator-only status updates, and unified authentication/authorization error responses.
- Added Redis account/IP failure windows, persistent login-attempt records containing only identifier hashes, 15-minute account locking after five failures, and 15-minute IP blocking after 20 failures.
- Hardened Nginx client-address forwarding so external callers cannot spoof the address used for IP limits.
- Added idempotent environment-driven initial administrator creation.
- Added login, registration, authenticated workspace, administrator authorization, and password-change pages.
- Added in-memory Zustand Access Token state and startup session restoration through the HttpOnly Refresh Token cookie.
- Added authentication architecture documentation, automated tests, JaCoCo enforcement, and an end-to-end authentication smoke script.

## 3. Incomplete work

None within Stage 02. Product data, CSV imports, data-source management, scoring, profit calculations, watchlists, alerts, dashboards with business metrics, and broader administrator workflows remain deliberately excluded until their authorized stages.

## 4. Changed files

- Backend authentication: `backend/src/main/java/com/tiktokinsight/auth/**` and `backend/src/test/java/com/tiktokinsight/auth/**`.
- Backend common/security: error codes, API exception handling, Spring Security configuration, application/test configuration, and health security test wiring.
- Database: `database/migrations/V1__create_auth_tables.sql`.
- Frontend routes: `/login`, `/register`, `/dashboard`, `/admin`, and `/profile`.
- Frontend authentication: API client, validation schemas, Zustand store, session bootstrap, protected route, application header, authentication shell, styles, and tests.
- Deployment: Docker environment wiring, Nginx trusted client-address forwarding, `.env.example`, and `deploy/scripts/auth-smoke-test.ps1`.
- Documentation: `docs/architecture/stage-02-auth.md` and this acceptance record.

## 5. Database migrations

- Added and executed `V1__create_auth_tables.sql`.
- Created `user_account`, `user_refresh_token`, and `login_attempt` with foreign keys, unique constraints, role/status checks, and lookup indexes.
- Flyway reported `success = 1`, version `1`, description `create auth tables`.
- Application restart validated the executed migration unchanged.
- Initial administrator record was verified as `admin@example.com / administrator / ADMIN / ENABLED` without reading or logging its password hash.

## 6. API changes

Added:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
PUT  /api/v1/auth/password
PUT  /api/v1/admin/users/{userId}/status
```

Public endpoints are limited to registration, login, refresh, logout, health, Actuator info/health, and OpenAPI. `/api/v1/admin/**` requires `ADMIN`; all other business API paths require authentication.

## 7. Configuration changes

- Added required `JWT_ACCESS_SECRET` and `JWT_REFRESH_SECRET` application bindings with minimum 32-character validation.
- Added fixed Access/Refresh lifetimes of two hours and 30 days.
- Added `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_USERNAME`, and `INITIAL_ADMIN_PASSWORD` bindings.
- Added `AUTH_COOKIE_SECURE` for HTTPS deployments.
- Passed authentication configuration into the backend container without embedding production secrets in source.
- Updated application stage metadata from foundation to authentication.

## 8. Test commands

```bash
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps

docker run --rm \
  -v tiktok_stage02_maven_cache:/root/.m2 \
  -v "$PWD:/workspace" \
  -w /workspace/backend \
  maven:3.9.11-eclipse-temurin-21 \
  ./mvnw -B -ntp clean verify

cd frontend
npm run lint
npm run typecheck
npm run test:coverage
npm run build
npm audit --omit=dev

powershell -ExecutionPolicy Bypass -File deploy/scripts/smoke-test.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/auth-smoke-test.ps1
```

Frontend commands were executed in the pinned `node:22-alpine` dependency image because the host terminal did not expose npm on `PATH`. Backend commands were executed with Java 21 in the pinned Maven image.

## 9. Actual test results

- Backend Java 21 Maven verification: `BUILD SUCCESS`.
- Backend unit tests: 21 run, 0 failures, 0 errors, 0 skipped.
- Backend MVC/authorization integration tests: 6 run, 0 failures, 0 errors, 0 skipped.
- JaCoCo authentication coverage check: all configured checks met.
- Frontend ESLint: passed with zero warnings.
- Frontend TypeScript check: passed.
- Frontend tests: 10 run, 10 passed.
- Frontend production build: passed; six application routes plus the not-found route were generated.
- Production dependency audit: 0 vulnerabilities.
- Docker Compose configuration and production image builds: passed.
- Flyway migration and restart validation: passed.
- Runtime health: backend, MySQL, Redis, and Nginx healthy; frontend running.
- Foundation health smoke test: passed with application, database, Redis, and storage all `UP`.
- Authentication smoke test: passed for registration, current user, normal-user admin rejection, Access/Refresh rotation, old Refresh Token rejection, logout revocation, five failed logins, and account lock enforcement.
- Browser QA: passed at desktop and 390 x 844 mobile viewports; administrator login, role redirect, session restoration after reload, and logout were verified with no console errors or warnings.

## 10. Coverage results

- Authentication core packages (`auth.application` and `auth.domain`) line coverage: 98.44% (126/128).
- Required authentication core line coverage: at least 80%; Maven now enforces this threshold.
- Overall backend line coverage: 67.21% (414/616).
- Frontend statement and line coverage: 32.07%.
- Frontend branch coverage: 90.69%.
- Frontend function coverage: 70.96%.
- Authentication validation and Zustand auth-store line coverage: 100%.

## 11. Performance test results

Stage 02 defines no formal throughput or latency threshold, so no synthetic load test was executed. Runtime authentication smoke tests completed successfully through Nginx, Spring Boot, Redis, and MySQL. Large-data performance testing remains assigned to later stages.

## 12. Acceptance criteria comparison

- [x] Registration.
- [x] Login.
- [x] Token refresh.
- [x] Refresh Token rotation.
- [x] Old Refresh Token invalidation.
- [x] Logout.
- [x] Current user query.
- [x] Normal and administrator roles.
- [x] Account enable/disable model and administrator-only status update.
- [x] Disabled users cannot log in or use existing Access/Refresh sessions.
- [x] Account failure threshold and 15-minute lock.
- [x] IP failure threshold and 15-minute block.
- [x] Initial administrator.
- [x] Password changes revoke all Refresh Tokens.
- [x] Normal users cannot access administrator APIs.
- [x] Login page.
- [x] Registration page.
- [x] Protected and role-restricted frontend routes.
- [x] Authentication core line coverage at least 80%.

## 13. Known issues

- Flyway 11.7.2 warns that MySQL 8.4 is newer than its latest tested MySQL 8.1 version. Migration, validation, restart, and runtime checks pass.
- MyBatis-Plus reports that no mapper is present. Stage 02 uses explicit JDBC repositories; product mappers remain a later-stage concern.
- Mockito reports that dynamic Java-agent loading will be restricted in a future JDK. Tests pass on Java 21.
- Maven Failsafe logs that it force-closes its forked JVM after successful tests; the test reports and Maven build complete successfully.
- The first uncached backend Docker dependency layer took approximately nine minutes on the available network. Subsequent builds use the cached layer.

## 14. Git branch

`feature/stage-02-auth`

## 15. Commit ID

Implementation commit: `3ff05f305ec782a08e6eddbfef7d72215df775eb`

## 16. Push result

Success. The implementation commit was pushed to `origin/feature/stage-02-auth`. The previously network-blocked Stage 01 `main` branch was also pushed successfully without merging Stage 02.

## 17. Prerequisites for the next stage

- The user must explicitly reply `本阶段验收通过`.
- Only after that approval may `feature/stage-02-auth` be merged into `main`.
- After merging, backend, frontend, Docker, migration validation, health, and authentication smoke tests must run again on `main` before Stage 03 begins.
- Until approval, do not implement products, imports, data sources, scoring, profit, watchlists, alerts, or later administrator workflows.
