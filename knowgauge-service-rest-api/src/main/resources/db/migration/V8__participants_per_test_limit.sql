-- A new workspace limit (ADR014): how many participants one test may have, counting every address invited to it or
-- that started an attempt (personal invitation or shared link). The system value comes from
-- kg.settings.tenant.participants-per-test; 0 means no limit.
INSERT INTO setting_configs (setting_key, setting_type)
VALUES ('kg.settings.tenant.participants-per-test', 'LIMIT');

-- Like the other limits (V3): shared libraries and application admins' own workspaces have no limits, so they get
-- their own value 0 for this one too. Sign-in and creating a library do the same from now on (TenantLimits.KEYS).
INSERT INTO settings (setting_owner, owner_id, setting_key, setting_value)
SELECT DISTINCT 'TENANT', t.id, 'kg.settings.tenant.participants-per-test', '0'
FROM tenants t
WHERE t.type = 'LIBRARY'
   OR t.id IN (SELECT u.tenant_id FROM users u JOIN user_roles r ON r.user_id = u.id AND r.role_code = 'ADMIN')
ON CONFLICT ON CONSTRAINT uq_settings_key DO NOTHING;
