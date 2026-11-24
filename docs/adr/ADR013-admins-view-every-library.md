# ADR013 - Admins view every library, read-only
* Date: Nov 24, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
An `ADMIN` (ADR012) can open any user's library and see it like the normal Library page: topics, documents,
preview and download. **The view is read-only.**

- **Terms.** A tenant is a **library** in the UI (my library, Alice's library, the shared library). A user's
  **workspace** is everything they work with: their own library plus the shared libraries. The code and the API keep
  "tenant".
- **List:** `GET /api/admin/tenants` (`ADMIN` only) returns every tenant with its type, owner and content counts.
- **Viewing reuses the content API.** There are no admin copies of the content endpoints. The admin sends the tenant
  id in the `X-KG-Tenant` header on `GET /api/content/**`. `AccountResolutionFilter` checks it, and
  `SignedInAccount.viewedTenantId` makes that tenant the current one for the request. `ContentScope.canWrite`
  answers `false` while viewing (`TenantViewProvider`), so every topic and document is reported `readOnly`.
- **Refused:** the header from a non-admin (`403`), with any method other than `GET`/`HEAD` (`403`), or for an
  unknown tenant (`404`). It is ignored outside `/api/content/**`, for the admin's own tenant and for a `LIBRARY`
  tenant (everyone reads that anyway, and an admin may change it).
- **UI:** an "Administration → Libraries" item in the sidebar, for admins only. It lists the libraries. Opening one
  shows `/admin/libraries/{tenantId}`: the Library page without the shared library or any edit or generate action.
  The admin's own and the shared library open the normal Library page. The UI clears cached topics and documents
  when it enters or leaves a viewed library.
- **Users are told.** The in-app text says "Only you and KnowGauge admins can see them", and the privacy page says
  administrators can view a library, read-only.

## Context
Admins need to look into users' libraries to support them and to run the beta, for example when ingestion or a test
goes wrong. Until now an admin could read only their own library and the shared one.

## Options Considered
### Option 1 (SELECTED): A header on the existing content API, read-only
#### Consequences
* Adopted because: no duplicated endpoints, and the UI reuses the whole Library page.
* Adopted because: read-only is enforced once, in the filter and in `ContentScope`, not per endpoint.
* Adopted despite: the header has to be scoped carefully: only content paths, only reads, only admins.

### Option 2: Admin copies of the content endpoints (`/api/admin/tenants/{id}/topics`, ...)
#### Consequences
* Rejected because: every content read would exist twice, and the UI would need a second set of hooks.

### Option 3: Full edit rights in other libraries
#### Consequences
* Rejected because: admins only need to look, and edits would change users' content without them knowing.

## Supporting Material
* [ADR012 – Roles are kept by KnowGauge](ADR012-roles-kept-by-knowgauge.md)
* [08 – REST API](../architecture/08-rest-api.md)
