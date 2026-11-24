-- Local profile only. Tenant 1 holds the seeded sample topics (R__dev_seed_topics.sql) and documents
-- (DevDocumentSeeder): it is the sample library (ADR010). Users who sign in get their own tenant and read the
-- library; with sign-in off (security mode fixed) requests run in tenant 1, which is how the samples are curated.
INSERT INTO tenants (id, type, name, owner_user_id)
VALUES (1, 'LIBRARY', 'KnowGauge Samples', NULL)
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, name = EXCLUDED.name, owner_user_id = NULL;

-- The library has no limits (ADR014): its own value 0 for each, like an admin's workspace. Kept if changed since.
INSERT INTO settings (setting_owner, owner_id, setting_key, setting_value)
SELECT 'TENANT', 1, k.setting_key, '0'
FROM (VALUES ('kg.settings.tenant.monthly-token-limit'),
             ('kg.settings.tenant.storage-limit-mb'),
             ('kg.settings.tenant.concurrent-generations-per-user'),
             ('kg.settings.tenant.participants-per-test')) AS k(setting_key)
ON CONFLICT ON CONSTRAINT uq_settings_key DO NOTHING;

-- Personal tenants created on sign-in get ids after the library.
SELECT setval(pg_get_serial_sequence('tenants', 'id'), GREATEST((SELECT MAX(id) FROM tenants), 1));
