# ADR014 - Settings per workspace, in one settings table
* Date: Nov 24, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
Settings have **levels**. A value is resolved the same way for every workspace (tenant):

**the workspace's own value → else the system value → else the properties files.**

- **One table, `settings`** (was `system_settings`, migrations `V3__settings_per_tenant.sql` and
  `V7__setting_configs.sql`): `setting_owner` (`SYSTEM` or `TENANT`), `owner_id` (NULL for `SYSTEM`, the tenant for
  `TENANT`), `setting_key`, `setting_value`. Constraints keep it consistent: a `SYSTEM` row has no owner id and a
  `TENANT` row has one, one row per (owner, owner id, key) (`UNIQUE NULLS NOT DISTINCT`), and a deleted tenant takes
  its rows with it (a trigger: `owner_id` has no foreign key, as later owners such as a billing plan will have ids
  of their own).
- **Every possible key is a row of `setting_configs`** (`setting_key`, `setting_type`), seeded by Flyway;
  `settings.setting_key` references it. `setting_type` is `CONFIG` (a configuration value, e.g. the default
  difficulty or question count) or `LIMIT` (a limit on what a workspace or user may use, e.g. storage, tokens,
  generations at once). Each `SettingDefinition` states its type too (`limit()`), and startup fails when a defined
  key is missing from `setting_configs` or has another type there. A new setting needs a migration adding its key.
  The System settings page shows each type on a vertical tab of its own.
- **Only workspace-level settings have `TENANT` rows.** A `SettingDefinition` says so (`tenantLevel`, set with
  `perTenant()`). For now these are the workspace limits: AI tokens per month, storage, tests generating at once
  per user. The models, batch sizes and similar stay system-only; saving one for a workspace is refused.
- **0 means no limit, at every level.** An empty value of a workspace means "use the system value", so "no limit"
  needs a value of its own. The system level uses 0 too, so there is one rule.
- **No special cases in code.** `TenantSettingsService.limits(tenantId)` gives the `TenantLimits` in effect, and
  every check and view uses it: the token check (`UsageServiceImpl`), uploads (`DocumentServiceImpl`), tests
  generating at once (`GenerationSlots`), the usage page, the limit banner and the admin's Workspaces list.
- **Workspaces without limits are data, not code.** The shared library (no users of its own: generating from its
  documents counts in the user's workspace) and an application admin's own workspace get their own value 0 for
  every limit: the migration for the existing ones, sign-in when it grants `ADMIN`
  (`AccountTransactionalServiceImpl`), and the local profile's library in `R__dev_library_tenant.sql`. An admin may
  change them like any other workspace's.
- **API and UI.** `GET`/`PUT`/`DELETE /api/admin/tenants/{id}/settings` list, save and reset a workspace's own
  values (`ADMIN` only). The Workspaces page has a **Limits** dialog per workspace and an "Own limits" badge; the
  System settings page marks workspace-level settings "Per workspace".

## Context
Workspace limits (ADR010 tenants; monthly AI tokens, storage, tests generating at once) started as system settings
that every workspace shared. Some workspaces need other values: an admin's own workspace should not be limited,
the shared library has no users of its own, and organization workspaces (later) will have limits of their own,
e.g. by plan.

## Options Considered
### Option 1 (SELECTED): one settings table with a level and a tenant
`SYSTEM` and `TENANT` rows in the same table, resolved by one rule.

#### Consequences
* Adopted because: one place and one rule for every setting value; a new workspace-level setting needs no schema
  change, only `perTenant()` on its definition.
* Adopted because: workspaces without limits (library, admins) are ordinary rows an admin can see and change, with
  no type or role checks in the limit code.
* Adopted because: room for more levels later (e.g. `USER`) or owners (e.g. a billing plan) without another table.
* Adopted despite: values are text, typed by their definition, not by the column.
* Adopted despite: `setting_owner` repeats what `owner_id IS NULL` says for `SYSTEM`; a check constraint keeps them
  in step.
* Adopted despite: a setting's key and type are written twice, in its definition and in `setting_configs`; the
  startup check keeps them in step.

### Option 2: limit columns on `tenants`
`monthly_token_limit`, `storage_limit_mb`, `concurrent_generations_per_user` on each tenant.

#### Consequences
* Rejected because: every new workspace-level setting is a schema change, and system and workspace values live in
  different places with different code.
* Rejected despite: typed columns.

### Option 3: skip limits in code for admins and the library
Check the caller's role and the tenant type in each limit check.

#### Consequences
* Rejected because: special cases in every check, and the list would not show why a workspace has no limit.
* Rejected despite: nothing to store.

## Supporting Material
* `knowgauge-service-rest-api/src/main/resources/db/migration/V3__settings_per_tenant.sql`
* `knowgauge-service-rest-api/src/main/resources/db/migration/V7__setting_configs.sql`
* `TenantSettingsService`, `SystemSettingsService`, `SettingDefinition`, `TenantLimits`
* ADR010 (tenants), ADR012 (roles), ADR013 (admins view every workspace)
