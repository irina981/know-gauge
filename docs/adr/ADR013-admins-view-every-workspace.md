# ADR013 - Admins view every workspace, read-only
* Date: Nov 24, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
An `ADMIN` (ADR012) can open any user's workspace, their library, tests and attempts, and see it on the normal
pages. **The view is read-only.**

- **Terms.** A tenant is a **library** in the UI (my library, Alice's library, the shared library). A user's
  **workspace** is everything they work with: their own library, tests and attempts, plus the shared libraries.
  The code and the API keep "tenant".
- **List:** `GET /api/admin/tenants` (`ADMIN` only) returns every tenant with its type, owner and content counts.
- **Viewing reuses the content API.** There are no admin copies of the content endpoints. The admin sends the tenant
  id in the `X-KG-Tenant` header on `GET` requests to the workspace APIs: everything under `/api/` except
  `/api/me`, `/api/admin/**` and `/api/public/**`. `AccountResolutionFilter` checks it, and
  `SignedInAccount.viewedTenantId` makes that tenant the current one for the request. `ContentScope.canWrite`
  answers `false` while viewing (`TenantViewProvider`), so every topic and document is reported `readOnly`.
- **Refused:** the header from a non-admin (`403`), with any method other than `GET`/`HEAD` (`403`), or for an
  unknown tenant (`404`). It is ignored outside the workspace APIs, for the admin's own tenant and for a `LIBRARY`
  tenant (everyone reads that anyway, and an admin may change it).
- **UI:** "Administration → Workspaces" in the sidebar, for admins only, lists the workspaces. Opening one shows its
  pages under `/admin/workspaces/{tenantId}/...`: the normal pages, without any edit, generate, publish, share or
  invite action, below a "Viewing … read-only" bar that has the workspace's Library, Tests and Attempts tabs. The
  sidebar, and with it the admin's own navigation (`/library`, `/tests`, `/attempts`), never changes. The admin's own
  and the shared library open the normal Library page. The UI clears cached workspace data on the way in and out.
- **Users are told.** The in-app text says "Only you and KnowGauge admins can see them", and the privacy page says
  administrators can view a library, read-only.

## Context
Admins need to look into users' workspaces to support them and to run the beta, for example when ingestion or a
test goes wrong. Until now an admin could read only their own workspace and the shared library.

## Options Considered
### Option 1 (SELECTED): A header on the existing content API, read-only
#### Consequences
* Adopted because: no duplicated endpoints, and the UI reuses the Library, Tests and Attempts pages.
* Adopted because: read-only is enforced once, in the filter and in `ContentScope`, not per endpoint.
* Adopted despite: the header has to be scoped carefully: only content paths, only reads, only admins.

### Option 2: Admin copies of the content endpoints (`/api/admin/tenants/{id}/topics`, ...)
#### Consequences
* Rejected because: every workspace read would exist twice, and the UI would need a second set of hooks.

### Option 3: Full edit rights in other libraries
#### Consequences
* Rejected because: admins only need to look, and edits would change users' content without them knowing.

### Option 4: Switch the whole app into the viewed workspace
#### Consequences
* Rejected because: the admin's own navigation would show someone else's data, which is confusing. Separate URLs
  keep the two apart.

## Supporting Material
* [ADR012 – Roles are kept by KnowGauge](ADR012-roles-kept-by-knowgauge.md)
* [08 – REST API](../architecture/08-rest-api.md)
