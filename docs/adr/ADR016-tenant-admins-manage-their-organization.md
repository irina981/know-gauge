# ADR016 - Tenant admins manage their organization workspace
* Date: Nov 24, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
A `TENANT_ADMIN` of an **organization** workspace (ADR015) manages it themselves, on two pages the sidebar shows
only to them, right before About:

- **Users** (`/workspace/users`): the users of the workspace with their roles, and its open invitations. They invite
  people by email as **`TENANT_ADMIN`, `TENANT_USER` or `TENANT_VIEWER`** (one role per invitation), resend and
  revoke invitations. Inviting an address again gives its pending invitation the new role. Joining works as in
  ADR015: the invitee leaves their current workspace and gets the invited role there.
- **Workspace settings** (`/workspace/settings`): the workspace's **configuration only**, the `CONFIG` settings
  with a `TENANT` owner (ADR014), e.g. the models and the defaults of new tests. Its **limits are neither shown nor
  changed** there: only an `ADMIN` sets them on the Workspaces page, and later a billing plan will. Resetting removes
  the workspace's own configuration values and keeps its limits.

The API is `/api/tenant/**` (`TenantController`), which needs the `TENANT_ADMIN` role. The core service
(`OrganizationTenantService`) works in the current tenant only and refuses, with `403`, a workspace that is not
an organization, any limit, and changes while an `ADMIN` views another workspace (ADR013). Personal workspaces and
libraries have neither page. An invitation never grants `ADMIN`, and a personal workspace still takes only
`TENANT_ADMIN` (its owner).

Every user also sees **their own roles** in Settings → Profile (from `/api/me`), each with what it allows.

**What each role may do** in the workspace is decided and enforced in ADR017 (a user changes only their own
tests and never the library; a viewer only reads).

## Context
ADR015 let only an `ADMIN` create workspaces and invite people, and gave every invitee `TENANT_ADMIN`, because the
other roles were not enforced (its Option 4). Organizations need to bring their own people in without asking an
application admin, and to choose their own models and test defaults. Limits are different: they cap cost and will
come from a billing plan, so the organization must not raise them itself.

## Options Considered
### Option 1 (SELECTED): Tenant admins manage users and configuration of their own organization
#### Consequences
* Adopted because: organizations invite their own people and set their own defaults without an application admin.
* Adopted because: limits stay with the application admin (later the billing plan), so cost stays capped.
* Adopted because: it reuses the invitation flow (ADR015) and the per-workspace settings (ADR014) unchanged.
* Adopted despite: `TENANT_USER` and `TENANT_VIEWER` can be given before they are enforced (ADR017 enforces them).

### Option 2: Let tenant admins also change their workspace's limits
#### Consequences
* Rejected because: a workspace could lift its own caps on tokens, storage and participants.

### Option 3: Offer only `TENANT_ADMIN` until the other roles are enforced
#### Consequences
* Rejected because: organizations want to record who is a user or a viewer now; enforcing the roles follows.

## Supporting Material
* [ADR012 – Roles are kept by KnowGauge](ADR012-roles-kept-by-knowgauge.md)
* [ADR013 – Admins view every workspace](ADR013-admins-view-every-workspace.md)
* [ADR014 – Settings per workspace](ADR014-settings-per-workspace.md)
* [ADR015 – Admins create workspaces and invite people to them](ADR015-admins-create-workspaces-and-invite.md)
