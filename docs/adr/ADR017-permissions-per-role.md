# ADR017 - What each role may do in a workspace
* Date: Nov 24, 2026
* Status: ACCEPTED (enforced in security mode `oidc`)
* Author: Irina Mijatovic

## Decision
Each user has one role in their workspace (tenant), kept in `user_roles` (ADR012). An organization's tenant admins
give it when they invite (ADR016); in a personal workspace the owner is always `TENANT_ADMIN`. The roles allow:

- **`TENANT_ADMIN`**: everything in the workspace, including its users, invitations and configuration.
- **`TENANT_USER`**: reads everything in the workspace; creates tests and changes **only the tests they created**;
  takes tests. Never changes the library.
- **`TENANT_VIEWER`**: **reads only**: the library, every test and every result. Changes nothing, takes no test.

`ADMIN` is not a workspace role: it is the application admin (ADR012). In their own workspace an `ADMIN` is also
`TENANT_ADMIN`. They change the shared libraries, and they read another user's workspace without changing it
(ADR013).

### Permissions

"Own" means a test the user created (`tests.created_by`). Reading always covers the workspace's own content plus
the shared libraries, which only an `ADMIN` changes (ADR010).

| Area | Action | `TENANT_ADMIN` | `TENANT_USER` | `TENANT_VIEWER` |
|---|---|---|---|---|
| Library | Read topics and documents, open PDFs | ✓ | ✓ | ✓ |
| Library | Create, rename, move or delete topics; import a topic tree | ✓ | – | – |
| Library | Upload, ingest or delete documents | ✓ | – | – |
| Tests | Read tests, questions, versions and history | ✓ | ✓ | ✓ |
| Tests | Generate a test | ✓ | ✓ | – |
| Tests | Review questions: accept a fix, regenerate, replace by hand, keep as is | any | own | – |
| Tests | Change details, publish, unpublish, delete | any | own | – |
| Sharing | Create or regenerate the shared link | any | own | – |
| Sharing | Invite participants, resend invitations | any | own | – |
| Attempts | Read attempts and results | ✓ | ✓ | ✓ |
| Attempts | Take a test in the app (start and submit an attempt) | ✓ | ✓ | – |
| Workspace | Users page: see users, invite as admin, user or viewer, resend, revoke (organization only) | ✓ | – | – |
| Workspace | Workspace settings: change the configuration (organization only, never the limits) | ✓ | – | – |
| Account | Own profile, roles, preferences and usage; accept an invitation to another workspace | ✓ | ✓ | ✓ |

### Endpoints

Everything not listed here only reads (`GET`) and is open to every role. Participants (`/api/public/**`, ADR009)
take tests without signing in and are not affected.

| Endpoint | Allowed for |
|---|---|
| `POST /api/content/topics`, `PUT /api/content/topics/{id}`, `DELETE /api/content/topics/{id}`, `POST /api/content/topics/tree` | `TENANT_ADMIN` |
| `POST /api/content/documents`, `DELETE /api/content/documents/{id}`, `POST /api/content/documents/ingestion/{id}` | `TENANT_ADMIN` |
| `POST /api/tests` | `TENANT_ADMIN`, `TENANT_USER` |
| `DELETE /api/tests/{id}` | `TENANT_ADMIN`; `TENANT_USER` for their own test |
| `POST /api/tests/{testId}/questions/{questionId}/accept-fix`, `…/dismiss-flags`, `…/regenerate`, `…/manual-replace` | `TENANT_ADMIN`; `TENANT_USER` for their own test |
| `PUT /api/tests/{testId}/details`, `POST /api/tests/{testId}/publish`, `POST /api/tests/{testId}/unpublish` | `TENANT_ADMIN`; `TENANT_USER` for their own test |
| `POST /api/tests/{testId}/shared-link` | `TENANT_ADMIN`; `TENANT_USER` for their own test |
| `POST /api/tests/{testId}/invitations`, `POST /api/tests/{testId}/invitations/{invitationId}/resend` | `TENANT_ADMIN`; `TENANT_USER` for their own test |
| `POST /api/tests/{testId}/attempts`, `POST /api/attempts/{attemptId}/submit` | `TENANT_ADMIN`, `TENANT_USER` |
| `/api/tenant/**` (all methods) | `TENANT_ADMIN` (organization only) |
| `PUT /api/me/preferences`, `POST /api/me/tenant-invitations/{token}/accept` | every role |

A refused action answers `403`. The UI hides the actions a role may not use (`usePermissions()`, and
`TestDto.readOnly` for a single test), but the server's check is the one that counts.

### How it is enforced
Two layers, so neither a forgotten annotation nor a new caller of a service opens a hole:

1. **On the controllers**: each endpoint that changes something names its roles with Jakarta
   `@RolesAllowed` (constants in `restapi.security.Roles`), as the endpoints table above lists. `MethodSecurityConfig`
   turns them on with `@EnableMethodSecurity(jsr250Enabled = true)`, in security mode `oidc` only. A refusal
   (`AccessDeniedException`) answers `403` in `GlobalControllerExceptionHandler`. `TenantController`
   (`/api/tenant/**`) carries `@RolesAllowed(TENANT_ADMIN)` on the class. `/api/admin/**` and the actuator keep their
   rule in `SecurityConfig`.
2. **In core** (`RolePermissions`, and `ContentScope.requireWritable` for the library): the same rules again, plus
   what an annotation cannot see, namely that a `TENANT_USER` changes only the tests they created. The user's roles
   come through the `CurrentRolesProvider` port. A refusal throws `PermissionDeniedException`, answered `403`.

- Reading is open to every role. `TestDto.readOnly` and the content DTOs' `readOnly` tell the UI what the caller may
  not change.
- Work outside a request (scheduled ingestion, startup seeding) runs as the system user and is not checked.
- With sign-in off (security mode `fixed`), the fixed user is `TENANT_ADMIN`, so nothing changes there.
- Limits (ADR014) still apply on top of the role: a user's generations count against the workspace's limits.

## Context
ADR015 offered only `TENANT_ADMIN` because nothing enforced the other roles. ADR016 let an organization's tenant
admins invite people as `TENANT_USER` or `TENANT_VIEWER`, which until now could do everything a tenant admin does
except open Users and Workspace settings. Organizations need people who build tests without reorganizing the shared
material, and people who only follow the results.

## Options Considered
### Option 1 (SELECTED): Users change tests, only their own; viewers only read
#### Consequences
* Adopted because: the library stays curated by the tenant admins, so users cannot remove material others rely on.
* Adopted because: a user cannot change or delete a colleague's test or its results.
* Adopted because: viewers can follow everything without risk.
* Adopted despite: a user who needs a new document must ask a tenant admin to upload it.

### Option 2: Users change everything except the Users and Workspace settings pages
#### Consequences
* Rejected because: anyone could delete documents and tests others depend on.

### Option 3: Users may change any test in the workspace
#### Consequences
* Rejected because: a test belongs to whoever builds it; tenant admins already cover handing it over.

### Option 4: Viewers may take tests in the app
#### Consequences
* Rejected because: an attempt is a change; viewers who should take a test get it as participants through a shared
  link or a personal invitation (ADR009).

## Supporting Material
* [ADR009 – Public test links](ADR009-public-test-links.md)
* [ADR010 – Keycloak sign-in and personal tenants](ADR010-keycloak-sign-in-and-personal-tenants.md)
* [ADR012 – Roles are kept by KnowGauge](ADR012-roles-kept-by-knowgauge.md)
* [ADR013 – Admins view every workspace](ADR013-admins-view-every-workspace.md)
* [ADR016 – Tenant admins manage their organization workspace](ADR016-tenant-admins-manage-their-organization.md)
