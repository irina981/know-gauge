# ADR007 - JSON Logs and Dozzle for the First Deployment
* Date: Nov 13, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

The first cloud deployment (`deploy/docker-compose.yml`, one server) uses:

- **JSON logs from the app** in the `prod` profile, using Spring Boot's built-in structured logging
  (`logging.structured.format.console=ecs`, Elastic Common Schema). `documentId` and `testId` are put into the
  MDC during ingestion and test generation, so they appear as fields.
- **Dozzle** as the log viewer, in both compose files: live tail, search, level filters and JSON lines shown as
  fields, for every container on the host. In `deploy/` it requires a login (`DOZZLE_AUTH_PROVIDER=simple`) and
  listens on `127.0.0.1` only; locally it has no login and also listens on `127.0.0.1`.
- **Docker log rotation** (3 × 20 MB per container) as the only retention.

## Context

The app logs plain text to the console, and the only way to read logs was `docker compose logs`. Before the first
deployment we want to browse, filter and read formatted logs in a browser, with little to run and maintain on a
single server. There is no need yet for long-term retention, dashboards or alerts.

## Options Considered

### Option 1 (SELECTED): Dozzle + JSON logs

One small container that reads the Docker socket, with no database and no configuration files.

#### Consequences
* Adopted because: it is one container, needs no storage, and shows all containers including the databases,
  MinIO and the UI's nginx.
* Adopted because: JSON (ECS) logs need no new dependency (Spring Boot 3.4+), and the same format works with Loki
  or Elastic later.
* Adopted despite: Dozzle keeps no history. It shows only what Docker still holds for each container (up to 60 MB),
  and nothing from removed containers, e.g. after a rebuild.
* Adopted despite: Dozzle needs the Docker socket, which gives full control of the host. Mitigated by the login and
  by listening on `127.0.0.1` only (reach it through an SSH tunnel or an HTTPS reverse proxy).

### Option 2: Grafana + Loki + Grafana Alloy

Logs are shipped by Alloy into Loki and queried in Grafana.

#### Consequences
* Rejected because: three more services plus configuration files for a single-server first deployment.
* Rejected despite: persistent history, LogQL queries, dashboards and alerts. This is the planned next step when
  those are needed; the JSON format stays the same. (Promtail, the older shipper, is end of life since
  March 2, 2026, so Alloy would be used.)

### Option 3: Elastic / OpenSearch stack

#### Consequences
* Rejected because: by far the heaviest option (several GB of memory) for one small app.

## Supporting Material
* Spring Boot structured logging: https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4
* Dozzle authentication: https://dozzle.dev/guide/authentication/simple
* Promtail end of life: https://grafana.com/docs/loki/latest/send-data/promtail/
