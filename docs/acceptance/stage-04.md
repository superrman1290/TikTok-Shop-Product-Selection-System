# Stage 04 acceptance: analysis, profit, and lifecycle

## 1. Stage objective

Deliver the `selection-v1.0` analysis workflow: market and user costs, persisted P75 category benchmarks, analysis jobs, versioned snapshots, profit calculation, trend/competition/profit/risk/selection scores, lifecycle classification, explanations, product analysis API, and analysis/profit pages. Stage 05 watchlists, monitoring rules, alerts, and dashboards remain excluded.

## 2. Completed work

- Added market default costs for all eight supported market currencies and user-scoped product-cost overrides.
- Added the immutable `selection-v1.0` algorithm with `BigDecimal` intermediate precision, HALF_UP final rounding, missing-metric re-normalization, fixed explanations, recommendation thresholds, and lifecycle precedence.
- Added nearest-rank P75 category benchmarks, durable analysis jobs, conditional claims, retry backoff, ShedLock protection, and versioned idempotent snapshots.
- Added authenticated analysis, cost-profile, and profit-calculation APIs.
- Added analysis scores, lifecycle, reasons, risks, an ECharts radar chart, score details, and a responsive personal-cost profit calculator to product pages.
- Added 18 algorithm/P75 test scenarios, including the required historical-data, P75, lifecycle, zero-base, missing metric, risk, profit, and recommendation boundaries.

## 3. Database migrations

- `V3__create_analysis_tables.sql` adds market/user costs, category benchmarks, analysis jobs, versioned snapshots, ShedLock, indexes, and default market costs.
- `V4__expand_analysis_rate_precision.sql` expands persisted raw growth/profit-rate precision. It was required after valid low-price products produced a formula-correct margin outside `DECIMAL(10,4)` storage range. Neither V1, V2, nor V3 was modified after execution.
- Real Flyway validation and migration succeeded: schema version 4, four migrations validated, all history rows successful.

## 4. API changes

```text
GET    /api/v1/products/{productId}/analysis
GET    /api/v1/products/{productId}/cost-profile
PUT    /api/v1/products/{productId}/cost-profile
DELETE /api/v1/products/{productId}/cost-profile
POST   /api/v1/products/{productId}/profit-calculations
```

All endpoints require authentication. Cost overrides are selected only by `user_id + product_id`; the market default is used when no override exists.

## 5. Test results

- Java 21 `mvn clean verify`: 50 unit tests and 11 integration tests passed. The final added algorithm scenario was also run in the focused suite, for 20 focused Stage 04 tests passed.
- `SelectionV1Algorithm` coverage: 99.49% lines (197/198) and 88.24% branches (90/102), exceeding the Stage 04 core-algorithm targets of 90% and 85%.
- Frontend lint, TypeScript check, 15 Vitest tests, and production build passed. The build contains the new `/products/[id]/profit` route.
- Real Java 21 runtime container completed Flyway validation/migration, reported all health components UP, and successfully executed analysis, market-cost resolution, personal-cost save/delete, and profit-calculation API calls.
- A strict 120-product fresh-snapshot run completed in 4.382 seconds, below the 30-second re-analysis requirement.

## 6. Golden cases

The executable golden cases and exact boundary expectations are documented in [selection-v1.0.md](../algorithms/selection-v1.0.md).

## 7. Known issues

- The production frontend image rebuilt successfully. The backend Docker image build remained blocked in Maven `dependency:go-offline` because the Docker builder could not complete Maven Central access. Java 21 Maven-container compile/package/verify and an equivalent Java 21 runtime container completed successfully against Compose MySQL and Redis.
- Flyway warns that MySQL 8.4 is newer than its latest tested MySQL 8.1 version. Validation, V3/V4 execution, restart behavior, and runtime APIs succeeded.

## 8. Scope boundary

Watchlists, monitoring rules, alerts, user dashboards, data-source key management, audit logs, and administrator task management were not implemented. They remain reserved for Stages 05 and 06.

## 9. Branch, commit, and push

Branch: `feature/stage-04-analysis`.

Implementation commit and push result are appended after the stage branch is committed and pushed.

## 10. Next prerequisite

Wait for the explicit user response `本阶段验收通过`. Only then may this branch be merged into `main`; all checks must be rerun on `main` before starting Stage 05.
