# ADR004 - Spring Profiles: local, test and prod
* Date: Nov 12, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

The `dev` and `docker` profiles are replaced by three profiles. `application.properties` keeps the values common
to all of them.

| Profile | Used for | Configuration |
|---|---|---|
| `local` | Development: the app from the IDE, and the app container in `local-development/docker-compose.yml` | `localhost` values and local credentials in the file. The compose container overrides the hosts through env vars (`SPRING_DATASOURCE_URL`, `KG_PGVECTOR_DATASOURCE_URL`, `KG_STORAGE_ENDPOINT`). Seed topics and seed PDFs |
| `test` | Automated tests only | Schema without seed data, no seeder, placeholder OpenAI key. Datasources come from the test (for example Testcontainers) |
| `prod` | Production | Every environment-specific value is an `${ENV_VAR}` placeholder. Required ones have no default, so a missing one fails startup. No seed data, Swagger off, actuator limited to `health,info,metrics` |

The `Dockerfile` sets `SPRING_PROFILES_ACTIVE=prod`, and compose and the IDE use `local`.

`DefaultTenantProvider` and `DefaultUserProvider` are no longer limited to a profile. They return
`kg.context.tenant-id` and `kg.context.user-id` (`KG_TENANT_ID` / `KG_USER_ID` in prod), so `prod` can start before
authentication exists. A deployment serves one tenant until then.

Ollama moved to the opt-in `ollama` compose profile, because distractor verification uses OpenAI (ADR003).

## Context

`dev` and `docker` differed only in hostnames and in where credentials came from, which env vars can override
anyway. Neither described production or tests. The tenant and user beans existed only in those two profiles, so no
production configuration could start.

## Options Considered

### Option 1 (SELECTED): local, test and prod; hosts in the container through env vars

#### Consequences
* Adopted because: one profile per environment, and the container and the IDE run the same local configuration.
* Adopted because: prod fails fast on a missing variable instead of starting with a wrong default.
* Adopted despite: the tenant is a configuration value, not a per-request value, until authentication exists.

### Option 2: keep a separate profile for the compose container

#### Consequences
* Rejected because: it duplicated the local file except for hostnames, which env vars already override.

## Supporting Material
* [09 – Configuration & deployment](../architecture/09-configuration-and-deployment.md)
* [ADR003 – Grounded, reason-first verification](ADR003-grounded-reason-first-verification.md)
