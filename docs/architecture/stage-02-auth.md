# Stage 02 architecture: authentication and authorization

## Scope

Stage 02 adds identity, session, role, account-status, and login-abuse controls. Product data, imports, scoring, profit, watchlists, alerts, and administration workflows remain outside this stage.

## Backend boundaries

The authentication module follows the repository layering contract:

```text
auth/api
  -> auth/application
  -> auth/domain
  -> auth/infrastructure
  -> MySQL / Redis
```

Controllers validate transport input, resolve the client address, call application services, and return the common API envelope. Transaction and credential decisions remain in application services. JDBC and Redis access remain behind domain ports.

## Session model

- Access Tokens are HS256 JWTs signed with `JWT_ACCESS_SECRET` and expire after two hours.
- Every Access Token includes a unique `jti`, issuer, subject, issue/expiry times, user identity, role, and an `access` token-type claim.
- Refresh Tokens are 48-byte cryptographically random opaque values and expire after 30 days.
- Only an HMAC-SHA256 digest keyed by `JWT_REFRESH_SECRET` is stored in MySQL.
- The raw Refresh Token is delivered only through an HttpOnly, SameSite Strict cookie scoped to `/api/v1/auth`.
- Refresh uses a database row lock, creates the replacement, and revokes the prior token in one transaction.
- Logout revokes the current Refresh Token. Password changes and account disablement revoke every active Refresh Token for that user.
- Access-token requests reload the current account so a disabled account is rejected immediately and current role changes take effect without waiting for JWT expiry.

## Login protection

Redis holds 15-minute counters keyed by SHA-256 digests of normalized account identifiers and client IP addresses:

```text
account: 5 failures -> persist a 15-minute account lock
IP:     20 failures -> reject login for the remaining window
```

Login attempts are also recorded in MySQL without plaintext email or IP values. Expected authentication failures do not roll back the attempt record or persisted account lock. Nginx overwrites `X-Forwarded-For` with the actual peer address so a client cannot supply the address used by rate limiting.

## Password and account rules

- BCrypt strength is 12.
- Password length is 8 to 64 characters with at least one ASCII letter and one digit.
- Email is normalized to lowercase and protected by a database unique constraint.
- Roles are `USER` and `ADMIN`; account statuses are `ENABLED` and `DISABLED`.
- Initial administrator creation is environment-driven and idempotent.

## Persistence

`V1__create_auth_tables.sql` creates:

- `user_account`
- `user_refresh_token`
- `login_attempt`

The migration defines foreign keys, unique token/email constraints, role/status checks, and lookup indexes. Executed migrations must not be edited.

## Frontend session flow

Zustand stores only the Access Token and current user in memory. On application startup, the frontend calls the refresh endpoint with credentials enabled; a valid HttpOnly cookie rotates the session and restores identity. React Hook Form and Zod enforce the same registration/password constraints as the backend. Protected routes show explicit loading and no-permission states, and admin pages require the `ADMIN` role.

## Verification boundaries

- JaCoCo enforces at least 80% line coverage for `auth.application` and `auth.domain` packages.
- Unit tests cover registration, failure thresholds, lockout, disablement, rotation, replay rejection, logout, and password revocation.
- MVC integration tests exercise unified errors, authentication, disabled accounts, and admin authorization.
- The authentication smoke script validates the complete flow through Nginx, Spring Boot, Redis, and MySQL.
