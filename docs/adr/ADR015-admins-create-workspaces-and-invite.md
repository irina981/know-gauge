# ADR015 - Admins create workspaces and invite people to them
* Date: Nov 24, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
An `ADMIN` (ADR012) creates a workspace (tenant) of any type on the Workspaces page and invites people to it by
email. **Membership stays as it is**: a user belongs to one tenant (`users.tenant_id`), and `user_roles` holds their
role in it. There is no membership table and no switching between workspaces.

- **Rules per type** (`TenantProvisioningServiceImpl`, `POST /api/admin/tenants`):
  - **Personal:** exactly one invitee. They become its owner (`owner_user_id`) when they accept.
  - **Organization:** at least one invitee. Every invitee becomes `TENANT_ADMIN`, the only role offered for now,
    because nothing enforces `TENANT_USER` or `TENANT_VIEWER` yet. An organization has no owner. (ADR016 later lets
    its tenant admins invite people as `TENANT_USER` or `TENANT_VIEWER` too.)
  - **Shared library:** no invitees, because libraries have no users (the V5 membership trigger). It gets its own
    value 0 for every limit (ADR014), and `ContentScope.refreshLibraries()` makes it readable by everyone at once.
- **Invitations** (table `tenant_invitations`, V6, renamed in V11) work like test invitations (ADR009):
  - Each invitation has one address and a random token.
  - The mail goes out between short transactions, never inside one. A failed send is kept with its error and can be
    resent.
  - Admins list, resend and revoke invitations (`/api/admin/tenants/{id}/invitations`) and can invite more people:
    any number to an organization, one to a personal workspace that has no owner and no invitation to someone else.
  - The link is `/join/{token}`. `GET /api/public/tenant-invitations/{token}` shows the workspace before sign-in.
- **Accepting** (`POST /api/me/tenant-invitations/{token}/accept`, `TenantJoiner`) needs the invited address,
  verified by Keycloak. It moves the user:
  - `users.tenant_id` becomes the invited workspace.
  - The `TENANT_*` roles are replaced by the invited role. `ADMIN` stays.
  - A personal workspace without an owner gets the user as its owner.
  - **The workspace the user had before keeps its content and has no users afterwards.** Admins can still open it
    read-only (ADR013). The UI warns about this before accepting.
  - `AccountResolver` drops the user's cached account, so the next request sees the new workspace.
- **First sign-in:** someone who has never signed in, and whose verified email has a pending invitation, joins that
  workspace (the oldest invitation) instead of getting an empty personal one. Their other invitations stay pending.
- **Filling a library:** an admin creates root topics and imports topic trees into a chosen library
  (`libraryTenantId` on the topic inputs). `TopicServiceImpl` allows this only for a library tenant, and only to whoever
  may change it (`ContentScope.requireWritable`). Sub-topics and uploads already followed their parent's tenant.

## Context
Until now a workspace only came into being at someone's first sign-in (a personal one), plus the libraries that
migrations create. Admins need to set up workspaces for people and companies before they arrive, and to add shared
libraries beside the samples. A user still works in exactly one workspace, which keeps every query scoped by one
`tenant_id` (ADR010).

## Options Considered
### Option 1 (SELECTED): One tenant per user; accepting moves the user
#### Consequences
* Adopted because: the security filter, `ExecutionContext` and every tenant-scoped query stay as they are.
* Adopted because: invitations reuse the patterns of test invitations (tokens, mail between transactions, resend).
* Adopted despite: an existing user who accepts leaves their old workspace. Its content stays, but they no longer
  see it. The UI and the email say so.

### Option 2: A membership table and a workspace switcher
#### Consequences
* Rejected because: every request would need a chosen workspace and a role check in it. That is a much larger change,
  and it is planned with Keycloak Organizations later.

### Option 3: Invite only people without an account
#### Consequences
* Rejected because: admins also need to bring existing users into an organization.

### Option 4: Offer `TENANT_USER` and `TENANT_VIEWER` to invitees now
#### Consequences
* Rejected because: nothing enforces them yet, so they would grant the same rights as `TENANT_ADMIN`.

## Supporting Material
* [ADR009 – Public test links](ADR009-public-test-links.md)
* [ADR010 – Keycloak sign-in and personal tenants](ADR010-keycloak-sign-in-and-personal-tenants.md)
* [ADR012 – Roles are kept by KnowGauge](ADR012-roles-kept-by-knowgauge.md)
* [ADR013 – Admins view every workspace](ADR013-admins-view-every-workspace.md)
* [ADR014 – Settings per workspace](ADR014-settings-per-workspace.md)
