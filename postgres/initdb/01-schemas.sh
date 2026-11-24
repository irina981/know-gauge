#!/bin/sh
# Runs once, when the Postgres data volume is first created (docker-entrypoint-initdb.d), in both
# local-development/ and deploy/. One database, three schemas:
#   knowgauge  the app's main data (Flyway db/migration), owned by POSTGRES_USER
#   vectors    chunk embeddings and the pgvector extension (Flyway db/pgvector-migration), owned by POSTGRES_USER
#   keycloak   Keycloak's tables, owned by its own login KEYCLOAK_DB_USER, which can use nothing else
# The app creates its two schemas itself if they are missing; Keycloak's login and schema exist only through here.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
	-v app_user="$POSTGRES_USER" \
	-v kc_user="${KEYCLOAK_DB_USER:-keycloak_user}" \
	-v kc_password="${KEYCLOAK_DB_PASSWORD:?Set KEYCLOAK_DB_PASSWORD}" <<'EOSQL'
CREATE SCHEMA IF NOT EXISTS knowgauge AUTHORIZATION :"app_user";
CREATE SCHEMA IF NOT EXISTS vectors AUTHORIZATION :"app_user";

CREATE ROLE :"kc_user" LOGIN PASSWORD :'kc_password';
CREATE SCHEMA keycloak AUTHORIZATION :"kc_user";
-- Keycloak looks only in its own schema.
ALTER ROLE :"kc_user" SET search_path TO keycloak;
EOSQL
