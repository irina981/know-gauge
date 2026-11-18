# ADR010 - Sign-in through Keycloak, one personal tenant per user
* Date: Nov 18, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
People sign in through **Keycloak** (OIDC). The service is an OAuth2 resource server: it validates the bearer
access token (issuer, signature, audience `knowgauge-api`) and maps the token's subject to a local `users` row.
On the first sign-in it creates the user **and a personal tenant** (`INDIVIDUAL`) for them. Every user belongs to
exactly one tenant, in which they are `TENANT_ADMIN`.

Two roles are assigned for now:

| Role | Where it lives | Grants |
|---|---|---|
| `APPLICATION_ADMIN` | Keycloak realm role (`realm_access.roles`) | `/api/admin/**` and the actuator endpoints other than health/info |
| `TENANT_ADMIN` | `users.role` | Everything in the user's own tenant |

`TENANT_USER` and `TENANT_VIEWER` (read-only member) are in the `Role` enum for organization tenants later, but
not assigned yet. The global admin user is created by the
**Keycloak realm import** on Keycloak's first start, with its email and password from `KG_ADMIN_EMAIL` /
`KG_ADMIN_PASSWORD`. The app creates the local row on that user's first sign-in, like for anyone else.

Sample content lives in a **`LIBRARY` tenant** (tenant type, not a setting and not "a tenant without owner"):
every user may read it and generate tests from it into their own tenant; it has no members and only an
`APPLICATION_ADMIN` may change it. Locally, tenant 1 (the seeded sample topics) is the library
(`R__dev_library_tenant.sql`). `ContentScope` implements this: content reads cover the own tenant and all
library tenants; topic and document changes (including ingestion) are allowed in the own tenant, and in the
library only for an `APPLICATION_ADMIN` (otherwise 403, `ReadOnlyContentException`). A sub-topic or document
always lives in its parent topic's tenant. `TopicDto` / `DocumentDto.readOnly` tell the UI to hide edit actions.
Jobs and seeding run as the system user in the library tenant itself, so it is their own tenant. Testers get their own account and tenant, not a shared demo login, so
usage can be tracked and limited per person.

Work that no person started (the scheduled ingestion job, startup seeding) runs as the reserved **system user**
(`users.id = 0`, no tenant, `ExecutionContext.SYSTEM_USER_ID`) through `ExecutionContext.runAsSystem(tenantId, …)`.
Participants who take a test through a public link stay anonymous: the tenant comes from the token's test,
invitation or attempt, and the audit user is `NULL`.

The behaviour is chosen with `kg.security.mode`:

- `fixed` (default for now, and the `test` profile): no sign-in, every request is allowed and runs as
  `kg.context.tenant-id` / `user-id`, exactly as before this ADR.
- `oidc`: tokens are required (except for `/api/public/**`, health/info, the landing page and Swagger).

## Context
The service had `tenant_id` on every table but took it from a fixed property, and had no authentication. A public
beta needs self-registration, password reset and per-user data. Keycloak gives registration, email
verification, password reset, brute-force protection and social logins for free, and its Organizations feature
fits company tenants later. The UI does not sign in yet, so the old behaviour has to keep working until it does.

## Options Considered
### Option 1 (SELECTED): Keycloak + local users table + personal tenant on first sign-in
#### Consequences
* Adopted because: no password handling in the app; tenants and users are ordinary rows the content can refer to.
* Adopted because: switching between `fixed` and `oidc` is a property, so the UI and tests keep working.
* Adopted despite: the user's email and name are copied from the token and can lag behind Keycloak by up to
  `kg.security.oidc.account-cache-minutes`.

### Option 2: Testers join the sample tenant, or share a read-only demo login
#### Consequences
* Rejected because: in the sample tenant every tester could change the samples and would see the others'
  uploads, tests and results; a shared demo login can't be tracked or limited per person, and testers could not
  try test generation.
* Rejected despite: it needs no query changes.

### Option 3: Admin user created by the app through the Keycloak Admin API
#### Consequences
* Rejected because: the app would need Keycloak credentials that can manage users.

### Option 4: The first person who signs in becomes the admin
#### Consequences
* Rejected because: on a fresh public deployment, whoever registers first gets admin rights.

## Advice
* All repository queries now filter by tenant (see `docs/architecture/10-cross-cutting-concerns.md`), with unit
  tests in `TenantScopingTest`. Add HTTP-level isolation tests (two users, two tenants) once integration tests
  exist, before `oidc` is used with more than one real user.

## Supporting Material
* Local realm: `local-development/keycloak/knowgauge-realm.json`
* Code: `restapi.security` (`SecurityConfig`, `AccountResolutionFilter`), `core.service.account`
* Schema: `tenants` and `users` in `V1__create_initial_schema.sql`; library tenant in `dev-migration/R__dev_library_tenant.sql`
