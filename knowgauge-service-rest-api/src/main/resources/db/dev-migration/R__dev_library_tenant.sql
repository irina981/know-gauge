-- Local profile only. Tenant 1 holds the seeded sample topics (R__dev_seed_topics.sql) and documents
-- (DevDocumentSeeder): it is the sample library (ADR010). Users who sign in get their own tenant and read the
-- library; with sign-in off (security mode fixed) requests run in tenant 1, which is how the samples are curated.
INSERT INTO tenants (id, type, name, owner_user_id)
VALUES (1, 'LIBRARY', 'KnowGauge Samples', NULL)
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, name = EXCLUDED.name, owner_user_id = NULL;

-- Personal tenants created on sign-in get ids after the library.
SELECT setval(pg_get_serial_sequence('tenants', 'id'), GREATEST((SELECT MAX(id) FROM tenants), 1));
