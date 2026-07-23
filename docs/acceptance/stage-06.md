# Stage 06 acceptance: administration and security

## 1. Stage objective

Deliver the administrator workspace and the security controls required to operate the product-selection system: administration dashboards and workflows, protected data-source credentials, audit trails, API rate limits, response headers, and sensitive-field masking.

## 2. Completed work

- Added the administrator dashboard, user list, role/status administration, product administration, analysis-job list, and all-active-product recalculation.
- Added data-source create, update, enable/disable, and list operations. Data-source secrets are encrypted before persistence with AES-256-GCM and are returned only as a tail hint such as `****1234`.
- Added audit persistence and query APIs. Audit records include operator identity, action/target identifiers, request ID/IP, result, detail, and creation time. Detail sanitization redacts password, token, secret, and authorization values.
- Audited user status/role changes, product edits, data-source mutations, analysis recalculation, and audit-log queries.
- Added Redis-backed per-IP API rate limiting, with a safe pass-through only when Redis is unavailable in isolated tests.
- Added `nosniff`, deny-frame, no-referrer, and permissions-policy security response headers.
- Added administrator pages for dashboard, users, data sources, jobs, and audit logs, with role-aware navigation and masked secret display.

## 3. Database migration

- `V6__create_administration_security_tables.sql` creates `data_source` and `audit_log`, plus the indexes needed for administrative job and audit-log queries.
- A real Flyway run against Compose MySQL applied V6 successfully. A subsequent application restart validated all six migrations.
- Runtime database inspection after creating a data source with `runtime-secret-1234` confirmed `encrypted_secret` contains no plaintext secret, has length 65, and stores the public `secret_hint` as `****1234`.

## 4. API changes

```text
GET     /api/v1/admin/dashboard
GET     /api/v1/admin/users
PUT     /api/v1/admin/users/{id}/role
PUT     /api/v1/admin/users/{userId}/status

GET     /api/v1/admin/products
GET     /api/v1/admin/products/{productId}
PUT     /api/v1/admin/products/{productId}

GET     /api/v1/admin/data-sources
POST    /api/v1/admin/data-sources
PUT     /api/v1/admin/data-sources/{id}
PUT     /api/v1/admin/data-sources/{id}/status

GET     /api/v1/admin/jobs/analysis
POST    /api/v1/admin/jobs/analysis/recalculate

GET     /api/v1/admin/audit-logs
```

All administration routes remain protected by the existing `ADMIN` role policy. The authentication security integration test verifies that a normal user receives the fixed forbidden code for an administrator route.

## 5. Test results

- Java 21 `mvn verify` passed: 57 unit tests and 9 integration tests, with JaCoCo coverage checks satisfied.
- Frontend checks in Node 22 passed: ESLint with zero warnings, TypeScript `tsc --noEmit`, and Vitest (4/4 tests).
- Real Java 21 runtime validation against Compose MySQL and Redis passed: health endpoint, administrator login, administration dashboard, data-source creation, masked API response, encrypted MySQL persistence, audit entries (`DATA_SOURCE_CREATED`, `AUDIT_LOG_QUERY`), and `X-Content-Type-Options: nosniff` response header.
- The full backend verification also re-ran `AuthSecurityIT` after adding the missing `AuditService` MVC-test mock: 5/5 tests passed.

## 6. Known issues

- Flyway warns that Compose MySQL 8.4 is newer than its tested MySQL 8.1 version. V6 migration, application startup, restart validation, and the Stage 06 runtime APIs succeeded.
- Surefire reports that its forked JVM needed forced termination 30 seconds after a successful integration-test exit. Maven still completed with exit code 0; all 9 integration tests and all coverage checks passed.
- Local Java is version 17 and npm is unavailable, so backend verification was run in a Java 21 Maven container and frontend verification in a Node 22 container.

## 7. Scope boundary

This stage delivers the requested administrator and security controls. Acceptance of this feature branch is required before any merge to `main`.

## 8. Branch and delivery

Branch: `feature/stage-06-admin-security`.

The implementation is committed and pushed only to this feature branch. It must not be merged into `main` until explicit Stage 06 acceptance is received.
