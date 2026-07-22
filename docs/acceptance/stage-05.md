# Stage 05 acceptance: user workflow

## 1. Stage objective

Deliver authenticated user workflow capabilities on top of the completed product-analysis system: personal watchlists, monitoring rules, daily alerts, an actionable dashboard, and product filtering for selection work. Stage 06 data-source credential management, audit logs, and administrator task management remain excluded.

## 2. Completed work

- Added user-scoped watchlists with add, remove, market-filtered pagination, duplicate protection, and product-detail entry points.
- Added alert rules for seven-day sales growth, seven-day price drops, competition-score increases, and selection-score drops.
- Added idempotent daily alert events, read-one/read-all workflows, user and market filtering, and preserved historical events when a rule is deleted.
- Added ShedLock-protected daily analysis compensation and daily alert evaluation jobs.
- Added an authenticated dashboard for market product counts, seven-day additions, recommended products, personal watchlist and unread-alert counts, sales-growth Top 10, and selection-score Top 10.
- Added server-side product filters for market, keyword, price range, lifecycle, recommendation, and minimum selection score.
- Added global market selection for the dashboard and product workspace, while keeping watchlists and alerts independently filterable across markets.

## 3. Database migration

- `V5__create_user_workflow_tables.sql` creates `watchlist`, `alert_rule`, and `alert_event`.
- The alert-event unique key is `(user_id, product_id, alert_rule_id, metric_type, stat_date)` to make daily evaluation idempotent.
- A real Flyway run against Compose MySQL validated all five migrations and applied V5 successfully. `flyway_schema_history` reports V5 success.

## 4. API changes

```text
GET     /api/v1/dashboard

GET     /api/v1/watchlist
POST    /api/v1/watchlist/{productId}
DELETE  /api/v1/watchlist/{productId}

GET     /api/v1/alert-rules
POST    /api/v1/alert-rules
PUT     /api/v1/alert-rules/{ruleId}
DELETE  /api/v1/alert-rules/{ruleId}

GET     /api/v1/alerts
POST    /api/v1/alerts/{alertId}/read
POST    /api/v1/alerts/read-all
```

All endpoints require authentication and scope data by the authenticated user. A repeated watchlist add returns `40902` (`PRODUCT_ALREADY_WATCHLISTED`).

## 5. Test results

- Java 21 `mvn verify` passed: 57 unit tests and 9 integration tests, with JaCoCo coverage checks satisfied.
- Added alert evaluation tests covering percentage and score-point thresholds, plus missing seven-day comparison data.
- Added watchlist tests covering authenticated-user scope, duplicate pairs, and invalid pagination.
- Frontend Vitest passed 4/4 tests; ESLint, TypeScript checking, and Next.js production build passed. The production build contains `/dashboard`, `/watchlist`, and `/alerts`.
- Real Java 21 runtime validation against Compose MySQL and Redis passed: Flyway V5, health (`application`, `database`, `redis`, and `storage` all `UP`), dashboard Top 10 lists, watchlist add/list/remove, duplicate-watchlist `409`, alert-rule create/list/delete, and alert-list access all succeeded. Test-created rules and watchlist entries were removed after validation.

## 6. Runtime correction

- During the initial runtime smoke test, the dashboard Top 10 query returned MySQL syntax error because Java text-block normalization removed the whitespace after `ORDER BY`. The query now inserts that separator explicitly. The full Java 21 verification and runtime smoke test were rerun after the correction.

## 7. Known issues

- Flyway warns that Compose MySQL 8.4 is newer than its tested MySQL 8.1 version. Validation, V5 execution, restart behavior, and the Stage 05 runtime APIs succeeded.
- Local Java is version 17 and npm is unavailable, so backend verification was run in a Java 21 Maven container and frontend verification in a Node 22 container.

## 8. Scope boundary

Data-source key management, credential secret handling, audit logging, and broader administrator task workflows are not part of Stage 05. They remain reserved for Stage 06.

## 9. Branch and delivery

Branch: `feature/stage-05-user-workflow`.

The implementation is committed and pushed only to this feature branch. It must not be merged into `main` until explicit Stage 05 acceptance is received.
