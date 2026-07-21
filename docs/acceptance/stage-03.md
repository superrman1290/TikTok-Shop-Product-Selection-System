# Stage 03 acceptance: products, imports, and data sources

## 1. Stage objective

Deliver category, shop, product, daily-stat, CSV/mock adapter, idempotent import, import-job/error, product ranking, basic detail/trend, product list/detail UI, administrator import UI, and administrator product-management capabilities without implementing analysis, profit, lifecycle, watchlist, alert, data-source secret-management, audit, or later-stage functionality.

## 2. Completed work

- Added category, shop, product, daily-stat, import-job, and row-level import-error persistence.
- Added the required `ProductDataSourceAdapter` boundary, strict streaming CSV adapters for products/statistics, and a deterministic mock adapter.
- Added generated-key local object storage for uploaded CSV files and synchronous import task state transitions.
- Enforced UTF-8, exact headers, `.csv` extension, MIME type, 20 MB file size, 200,000 rows, bounded error values, supported market/currency pairs, and sensitive-field redaction.
- Added 1,000-row JDBC batches, dimension/product/stat idempotency, missing-product row errors, and latest-stat-date/current-price handling.
- Added authenticated product pages with bounded server pagination, title/market filters, a fixed SQL sort whitelist, product details, and ordered daily statistics.
- Added administrator product listing/edit APIs and pages, separate product/stat uploads, import task results, and row-level error details.
- Added ECharts price/sales trends, real product image/fallback states, responsive operational layouts, and compact mobile navigation.
- Added deterministic functional/performance data generation and reusable Stage 03 smoke/performance scripts.
- Added Stage 03 architecture documentation and updated repository usage instructions.

## 3. Incomplete work

None within Stage 03. Analysis scores, profit calculation, lifecycle recognition, benchmarks, snapshots, user costs, watchlists, monitoring, alerts, data-source key management, broader task management, audit logs, and dashboards remain deliberately excluded for their authorized later stages.

## 4. Changed files

- Backend product catalog: `backend/src/main/java/com/tiktokinsight/product/**`.
- Backend adapters and normalized inputs: `backend/src/main/java/com/tiktokinsight/datasource/**`.
- Backend imports and persistence: `backend/src/main/java/com/tiktokinsight/importing/**`.
- Backend shared API/configuration: page response, Stage 03 error codes, multipart handling, banner/stage metadata, and Apache Commons CSV.
- Backend tests: product, data-source, importing, real-MySQL persistence, and security tests under `backend/src/test/java/com/tiktokinsight/**`.
- Database: `database/migrations/V2__create_product_tables.sql`.
- Frontend: `/products`, `/products/[id]`, `/admin/products`, `/admin/imports`, shared product API/image/chart components, navigation, responsive styles, and tests.
- Deployment and tools: Nginx upload/timeouts, Stage 03 smoke/performance scripts, and `tools/generate-mock-data.mjs`.
- Documentation: `README.md`, `docs/architecture/stage-03-product-data.md`, and this acceptance record.

## 5. Database migrations

- Added and successfully executed `V2__create_product_tables.sql` without modifying executed `V1`.
- Created `category`, `shop`, `product`, `product_daily_stat`, `import_job`, and `import_job_error` with required foreign keys, checks, unique constraints, and lookup/ranking indexes.
- Flyway history reports version `2`, description `create product tables`, `success = 1`, checksum `-332424678`.
- Final restart reported two migrations successfully validated, current schema version 2, and no migration necessary.
- The first local V2 attempt exposed MySQL 8.4 treating `row_number` as a reserved expression. Because V2 had not succeeded, only its failed partial tables/history row were removed; the unexecuted migration was corrected to `row_index` and then executed successfully. V1 was untouched.

## 6. API changes

Added user APIs:

```text
GET /api/v1/products
GET /api/v1/products/{productId}
GET /api/v1/products/{productId}/stats
```

Added administrator APIs:

```text
GET  /api/v1/admin/products
GET  /api/v1/admin/products/{productId}
PUT  /api/v1/admin/products/{productId}

POST /api/v1/admin/imports/products
POST /api/v1/admin/imports/product-stats
GET  /api/v1/admin/imports
GET  /api/v1/admin/imports/{importId}
GET  /api/v1/admin/imports/{importId}/errors
```

Product pages are bounded to page sizes from 1 to 100. Sort values map through a fixed enum; client values are never used as raw SQL identifiers. Normal business APIs require authentication and all administrator APIs require `ADMIN`.

## 7. Configuration changes

- Added Spring multipart limits of 20 MB per file and 21 MB per request.
- Increased Nginx request-body allowance to 21 MB for multipart overhead.
- Added five-minute Nginx upstream send/read timeouts for large synchronous imports.
- Updated application stage metadata and banner to product data.
- Added Apache Commons CSV 1.14.1.
- No secrets or production credentials were added to source.

## 8. Test commands

```powershell
docker compose config --quiet
docker compose build
docker compose up -d --force-recreate backend frontend nginx
docker compose ps

docker run --rm `
  --network tiktok-product-insight_app `
  -e TEST_MYSQL_HOST=mysql `
  -e TEST_MYSQL_PASSWORD=change_me `
  -v tiktok_stage03_maven_cache:/root/.m2 `
  -v "${PWD}:/workspace" `
  -w /workspace/backend `
  maven:3.9.11-eclipse-temurin-21 `
  mvn -B -ntp clean verify

docker run --rm `
  -v "${PWD}\frontend:/workspace" `
  -v tiktok_stage03_node_modules:/workspace/node_modules `
  -w /workspace node:22-alpine npm run lint
docker run --rm `
  -v "${PWD}\frontend:/workspace" `
  -v tiktok_stage03_node_modules:/workspace/node_modules `
  -w /workspace node:22-alpine npm run typecheck
docker run --rm `
  -v "${PWD}\frontend:/workspace" `
  -v tiktok_stage03_node_modules:/workspace/node_modules `
  -w /workspace node:22-alpine npm run test:coverage
docker run --rm `
  -v "${PWD}\frontend:/workspace" `
  -v tiktok_stage03_node_modules:/workspace/node_modules `
  -w /workspace node:22-alpine npm run build
docker run --rm `
  -v "${PWD}\frontend:/workspace" `
  -v tiktok_stage03_node_modules:/workspace/node_modules `
  -w /workspace node:22-alpine npm audit --omit=dev

powershell -ExecutionPolicy Bypass -File deploy/scripts/smoke-test.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/auth-smoke-test.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/stage-03-smoke-test.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/stage-03-performance-test.ps1
```

## 9. Actual test results

- Backend Java 21 Maven verification: `BUILD SUCCESS`.
- Backend unit tests: 32 run, 0 failures, 0 errors, 0 skipped.
- Backend MVC/security/real-MySQL integration tests: 10 run, 0 failures, 0 errors, 0 skipped.
- Real-MySQL tests verified product/category/shop idempotency, daily-stat idempotency, missing-product errors, duplicate-stat updates, and out-of-order statistics not regressing the latest price/date.
- Frontend ESLint: passed with zero warnings.
- Frontend TypeScript check: passed.
- Frontend tests: 15 run, 15 passed.
- Frontend production build: passed with 10 application routes plus the not-found route.
- Production dependency audit: 0 vulnerabilities.
- Docker Compose configuration and final backend/frontend image builds: passed.
- All final containers are running; backend, MySQL, Redis, and Nginx report healthy.
- Foundation health smoke test: passed.
- Authentication smoke test: passed.
- Stage 03 smoke test: passed for 120 product rows, 3,600 statistic rows, repeated product import, row-level errors, market ranking, detail, trends, and normal-user import rejection.
- Browser QA passed on desktop and 390 x 844 mobile layouts for product ranking/search, detail/image/trend chart, administrator imports/errors, and product edit UI. Mobile detail `scrollWidth` equals `clientWidth`; no console errors or warnings were present.

## 10. Coverage results

- Authentication core packages: 98.44% line coverage (126/128); the enforced 80% gate passed.
- Stage 03 product/importing/data-source packages: 59.23% line coverage (475/802).
- Overall backend: 62.91% line coverage (899/1,429).
- Frontend statements and lines: 22.38%.
- Frontend branches: 89.06%.
- Frontend functions: 66.66%.
- Product API helper statements/lines: 74.19%; product image component: 100%.

Stage 03 defines no separate coverage threshold. Security, adapter, batching, idempotency, latest-price, and real-database behavior are covered by focused tests.

## 11. Performance test results

- Fixed seed: `20260721`.
- 10,000-row product CSV size: approximately 1.35 MiB.
- 100,000-row product CSV size: approximately 13.83 MiB, below the 20 MB limit.
- 10,000-row streaming import: 20.156 seconds, status `SUCCESS`, 10,000 successful rows, 0 failed rows.
- 100,000-row idempotent product processing: 101.736 seconds, status `SUCCESS`, 100,000 successful rows, 0 failed rows.
- Backend memory after the performance run: approximately 394.8 MiB; no restart or out-of-memory event occurred.
- Product table cardinality after idempotent imports: 100,000.
- Product-list first-page measurement: 5 warmups plus 30 samples through Nginx/API/MySQL.
- Product-list first-page P95: 32.32 ms, below the required 2,000 ms.
- MySQL `EXPLAIN` selected `idx_product_market_status_collected`, used a backward index scan for the ordered product page, and used primary-key `eq_ref` joins for category and shop.
- Required product, daily-stat, and import-job indexes were queried from `information_schema.statistics` and found.
- Local evidence files were written under ignored `tmp/performance-data/` and were not committed.

## 12. Acceptance criteria comparison

- [x] Categories.
- [x] Shops.
- [x] Products.
- [x] Daily statistics.
- [x] CSV product adapter.
- [x] CSV daily-stat adapter.
- [x] Deterministic mock adapter.
- [x] Product import.
- [x] Daily-stat import.
- [x] Import jobs.
- [x] Row-level import errors with row number, field, raw value, code, and message.
- [x] Product idempotency by platform, market, and external product ID.
- [x] Daily-stat idempotency by product and statistic date.
- [x] Product ranking with safe server-side pagination and sorting.
- [x] Product basic detail.
- [x] Product trend API and ECharts view.
- [x] Product list page.
- [x] Product detail basic page.
- [x] Administrator import page.
- [x] Administrator product management.
- [x] UTF-8, file-size, row-count, extension, MIME, storage-key, and error-redaction controls.
- [x] 10,000-row streaming import without out-of-memory failure.
- [x] 1,000-row database batches.
- [x] 100,000-product first-page P95 at or below 2 seconds.
- [x] SQL `EXPLAIN` supplied and required indexes verified.
- [x] Later-stage functionality excluded.

## 13. Known issues

- Flyway 11.7.2 warns that MySQL 8.4 is newer than its latest tested MySQL 8.1 version. Migration execution, validation, restart, real-MySQL integration tests, and runtime queries pass.
- Mockito warns that dynamic Java-agent loading will be restricted in a future JDK. Tests pass on Java 21.
- Docker-to-Maven-Central connectivity was intermittent. One final backend image attempt failed before compilation while Central was unreachable; an HTTP 200 connectivity check immediately preceded a successful retry of the identical source. Independent Maven verification and the final Compose images passed.
- The ECharts detail route has the largest frontend bundle in this stage. It remains functional on tested desktop/mobile viewports; bundle optimization is not a Stage 03 acceptance criterion.

## 14. Git branch

`feature/stage-03-product-data`

## 15. Commit ID

Implementation commit: `4d37e889543e7cfc56e447de1148d6c8cefbaf6b`

## 16. Push result

Success. The implementation commit was pushed to `origin/feature/stage-03-product-data`.

## 17. Prerequisites for the next stage

- The user must explicitly reply `本阶段验收通过`.
- Only after that approval may `feature/stage-03-product-data` be merged into `main`.
- After merging, backend, frontend, Docker, Flyway restart validation, health, authentication, Stage 03 smoke, and product-list performance checks must be rerun on `main` before Stage 04 begins.
- Until approval, do not implement analysis, profit, lifecycle, benchmarks, snapshots, watchlists, alerts, or any later-stage functionality.
