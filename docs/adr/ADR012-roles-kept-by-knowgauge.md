# ADR012 - Roles are kept by KnowGauge, Keycloak only authenticates
* Date: Nov 24, 2026
* Status: ACCEPTED (replaces the role part of ADR010)
* Author: Irina Mijatovic

## Decision
**Authorization belongs to KnowGauge.** Keycloak only authenticates: it proves who the user is (subject, email,
`email_verified`, name) and knows nothing about KnowGauge's roles. The app ignores any roles in the token.

- Table **`roles`** lists every role the app knows (the `Role` enum): `ADMIN`, `TENANT_ADMIN`, `TENANT_USER`,
  `TENANT_VIEWER`.
- Table **`user_roles`** (`user_id`, `role_code`, `granted_at`) holds each user's roles, so a user can have several.
  The single `users.role` column is gone. Both tables are in `V2__users_and_tenants.sql`.
- The application admin role is now **`ADMIN`** (was the Keycloak realm role `APPLICATION_ADMIN`). It guards
  `/api/admin/**`, the actuator endpoints other than health/info, and changes to the shared library.
- On the first sign-in a user gets `TENANT_ADMIN` of their new personal tenant.
- **The first admin:** a user whose **verified** email is listed in `kg.account.admin-emails` (`KG_ADMIN_EMAIL`)
  gets `ADMIN` when they sign in. This only adds the role; nothing is revoked when an email leaves the list.
- `AccountResolutionFilter` turns the user's roles into the request's authorities (`ROLE_ADMIN`,
  `ROLE_TENANT_ADMIN`). `/api/me` returns them in `roles`.

## Context
ADR010 split the roles across two systems. `APPLICATION_ADMIN` was a Keycloak realm role read from the token on
each request. `TENANT_ADMIN` was a column on the user row. So the database never showed who the admins are, a user
had at most one stored role, and granting admin meant changing Keycloak. The plan is to stay independent of the
identity provider, which may change later, and to allow more roles per user with organization tenants.

## Options Considered
### Option 1 (SELECTED): Roles only in KnowGauge's database
#### Consequences
* Adopted because: one place answers "who may do what", it can be queried and audited, and it moves with the
  data, not with the identity provider.
* Adopted because: a user can hold several roles; organization tenants can add tenant roles later.
* Adopted despite: the first admin needs a bootstrap rule (`kg.account.admin-emails`), and there is no screen yet
  to grant or revoke roles. Until there is, it is done in SQL on `user_roles`.
* Adopted despite: a role change applies once the cached account expires (`kg.security.oidc.account-cache-minutes`,
  10 minutes).

### Option 2: Keycloak decides, the app copies the token's roles into `user_roles`
#### Consequences
* Rejected because: authorization would still depend on Keycloak's configuration.
* Rejected despite: admins could be managed in the Keycloak console without new app screens.

## Supporting Material
* [ADR010 – Sign-in through Keycloak](ADR010-keycloak-sign-in-and-personal-tenants.md)
* [08 – REST API](../architecture/08-rest-api.md)
