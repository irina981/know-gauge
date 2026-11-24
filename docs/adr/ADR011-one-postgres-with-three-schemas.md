# ADR011 - One Postgres server, three schemas: knowgauge, vectors and keycloak
* Date: Nov 24, 2026
* Status: ACCEPTED
* Author: Irina Mijatovic

## Decision
KnowGauge runs on **one PostgreSQL server with one database** (`knowgauge`, image `pgvector/pgvector`) in both
`local-development/` and `deploy/`. The database has three schemas:

| Schema | Holds | Owner | Created by |
|---|---|---|---|
| `knowgauge` | The app's relational data (`db/migration`, `db/dev-migration`) | `POSTGRES_USER` | `postgres/initdb`, else the main Flyway |
| `vectors` | `chunk_embeddings` and the `vector` extension (`db/pgvector-migration`) | `POSTGRES_USER` | `postgres/initdb`, else the pgvector Flyway |
| `keycloak` | Keycloak's tables | `KEYCLOAK_DB_USER` (its own login, no access to the other schemas) | `postgres/initdb` |

The app keeps its **two datasources, persistence units and transaction managers**. Each datasource URL selects its
schema with `?currentSchema=knowgauge` / `?currentSchema=vectors`, and each Flyway keeps its history in its own
schema (`spring.flyway.schemas`, `kg.pgvector.flyway.schemas`). Keycloak uses `KC_DB_SCHEMA=keycloak`.

`postgres/initdb/01-schemas.sh` runs once, when the data volume is created. It creates the schemas and Keycloak's
login.

## Context
Until now there were two Postgres containers (main data and vectors), and Keycloak kept its data in a file
database in the local compose file only. `deploy/` had no Keycloak at all. The release plan targets one small
server (4 vCPU / 8 GB). A second Postgres costs about 500 MB of RAM, a second backup job and a second set of
credentials, for data that is tiny next to the main data. Keycloak needs a real database in production.

## Options Considered
### Option 1 (SELECTED): One database, three schemas
#### Consequences
* Adopted because: one container, one volume, and one `pg_dump` backs up the app, the embeddings and the users
  together, consistent with each other.
* Adopted because: the app's code does not change. The embeddings stay a separate persistence unit, with no
  foreign keys to the main data, so they can still move to a dedicated vector store later.
* Adopted because: Keycloak's login can use only its own schema.
* Adopted despite: the app and the vectors share one login, and one heavy query can slow the others. Both are
  acceptable at beta scale.
* Adopted despite: the existing local volumes (`postgres_data`, `postgres_vectors_data`, `keycloak_data`) are not
  migrated. The new volume is `db_data`; the local dev seed fills it again.

### Option 2: One server, three databases
#### Consequences
* Rejected because: three dumps to schedule and restore together, and the pgvector extension per database.
* Rejected despite: stronger separation (`CONNECT` per database).

### Option 3: Keep separate servers
#### Consequences
* Rejected because: more memory, containers and backups on a single small server.

## Supporting Material
* [07 – Persistence & Storage](../architecture/07-persistence-and-storage.md)
* [09 – Configuration & Deployment](../architecture/09-configuration-and-deployment.md)
* [ADR010 – Sign-in through Keycloak](ADR010-keycloak-sign-in-and-personal-tenants.md)
