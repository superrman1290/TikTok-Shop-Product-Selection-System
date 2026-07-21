# Stage 03 product data architecture

## Scope

Stage 03 adds product catalog storage, read APIs, CSV and mock source adapters, synchronous streaming imports, row-level import diagnostics, product administration, server-paginated frontend views, and deterministic functional/performance data generation. Analysis scores, profit, lifecycle, watchlists, alerts, data-source secret management, and later administrator modules remain outside this stage.

## Module boundaries

```text
datasource
  CsvProductDataSourceAdapter
  CsvProductStatDataSourceAdapter
  MockProductDataSourceAdapter
  immutable normalized import records

importing
  ImportApplicationService
  ImportJobRepository
  ProductImportRepository
  JDBC batch and task persistence

product
  ProductCatalogService
  ProductCatalogRepository
  user and administrator REST controllers
```

Adapters only read, validate, and normalize external rows. `ImportApplicationService` owns upload security, object storage, batching, counters, error persistence, and task status. JDBC repositories own idempotent writes and the latest-price rule. Controllers only bind requests and wrap responses.

## Database migration

`V2__create_product_tables.sql` creates:

- `category`, unique by platform, market, and external ID.
- `shop`, unique by platform, market, and external ID.
- `product`, unique by platform, market, and external product ID.
- `product_daily_stat`, unique by product and statistic date.
- `import_job` and `import_job_error`.

Money uses `DECIMAL(18,2)` and Java `BigDecimal`; ratios use `DECIMAL(10,4)`. Product list indexes cover market/category/status/price, market/category/listed time, status/latest statistic date, and the default market/status/collected-time ranking. Daily statistic indexes support both product history and date-oriented access.

## Import pipeline

```text
validate extension, MIME, size
  -> store under a generated object key
  -> create RUNNING import job
  -> stream UTF-8 CSV rows
  -> normalize and validate fields
  -> collect at most 1,000 valid rows
  -> batch-upsert dimensions and products/statistics
  -> persist row-level errors in bounded batches
  -> complete as SUCCESS or PARTIAL_SUCCESS
```

The upload limit is 20 MB and the row limit is 200,000. A strict UTF-8 decoder rejects malformed input. Headers must exactly match the documented field set. Supported markets and currencies use one centralized enum. Error raw values are capped at 500 characters and sensitive-looking fields are redacted.

Product imports update existing basics, including the imported current price. Daily-stat imports update `product.current_price` and `latest_stat_date` only when the incoming date is not older than the current latest date. Historical consumers read `product_daily_stat.price`, not the redundant current price.

## Read and administration APIs

User product APIs expose bounded server pagination, market/category/title filters, a fixed sort whitelist, product basics, and up to 366 ordered daily statistics. Public business access still requires authentication and only `ACTIVE` products are returned.

Administrator APIs expose all product statuses, basic field/status updates, product/stat CSV imports, import job pages, and row-level errors. `/api/v1/admin/**` remains protected by backend role checks; the frontend also hides administrator routes from normal users.

## Frontend

- `/products` provides server pagination, title search, market selection, and ranking controls.
- `/products/[id]` provides product basics and an ECharts price/sales trend.
- `/admin/products` provides all-status listing and an edit modal.
- `/admin/imports` provides separate product/stat uploads, task status, counts, and error details.

All pages include loading, empty, error, and role-denied states. The desktop layout is optimized for operational scanning; tables use controlled horizontal scrolling on narrow screens. The mobile header uses fixed icon-button dimensions so navigation cannot expand the page width.

## Performance strategy

The deterministic generator uses seed `20260721`. Performance product CSV output omits optional media URLs so 100,000 rows remain below the 20 MB security limit. Imports read one row at a time and retain only the active 1,000-row batch plus bounded error buffers. The default product ranking has a dedicated composite index and is verified with MySQL `EXPLAIN`.
