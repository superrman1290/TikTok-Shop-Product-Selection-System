# Database migrations

Flyway scans this directory at application startup. Stage 01 intentionally contains no business schema. New versioned migrations are added by the stage that owns the corresponding tables, and committed migrations are immutable.
