-- Settings at two levels (ADR014): one table for system-wide values and per-tenant (workspace) values.
-- A SYSTEM row overrides the properties files; a TENANT row overrides the system value for one tenant. Only settings
-- marked as workspace-level (SettingDefinition.workspaceLevel) may have TENANT rows, for now the workspace limits.

ALTER TABLE system_settings RENAME TO settings;
ALTER TABLE settings RENAME CONSTRAINT system_settings_pkey TO settings_pkey;
ALTER TABLE settings DROP CONSTRAINT uq_system_settings_key;

ALTER TABLE settings
    ADD COLUMN setting_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN tenant_id BIGINT NULL;
ALTER TABLE settings ALTER COLUMN setting_type DROP DEFAULT;

ALTER TABLE settings
    ADD CONSTRAINT chk_settings_type CHECK (setting_type IN ('SYSTEM', 'TENANT')),
    -- a SYSTEM row has no tenant, a TENANT row always has one
    ADD CONSTRAINT chk_settings_tenant CHECK ((setting_type = 'SYSTEM') = (tenant_id IS NULL)),
    -- a deleted tenant takes its settings with it
    ADD CONSTRAINT fk_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    -- one value per key and level; NULLS NOT DISTINCT so SYSTEM rows (tenant_id NULL) are unique too
    ADD CONSTRAINT uq_settings_key UNIQUE NULLS NOT DISTINCT (setting_type, tenant_id, setting_key);

CREATE INDEX idx_settings_tenant_id ON settings(tenant_id);

-- The workspace limits take 0 for "no limit" now (an empty TENANT value would be ambiguous: it means "use the
-- system value"). Saved empty system limits become 0.
UPDATE settings SET setting_value = '0'
WHERE setting_type = 'SYSTEM' AND (setting_value IS NULL OR setting_value = '')
  AND setting_key IN ('kg.settings.tenant.monthly-token-limit', 'kg.settings.tenant.storage-limit-mb',
                      'kg.settings.tenant.concurrent-generations-per-user');

-- Every workspace's limits come from the same rule: its own value, else the system value; 0 means no limit. Shared
-- libraries (no users of their own: work with their documents counts in the user's workspace) and application
-- admins' own workspaces get their own value 0. Sign-in does the same when it grants ADMIN later; the local
-- profile's library gets them in R__dev_library_tenant.sql.
INSERT INTO settings (setting_type, tenant_id, setting_key, setting_value)
SELECT DISTINCT 'TENANT', t.id, k.setting_key, '0'
FROM tenants t
CROSS JOIN (VALUES ('kg.settings.tenant.monthly-token-limit'),
                   ('kg.settings.tenant.storage-limit-mb'),
                   ('kg.settings.tenant.concurrent-generations-per-user')) AS k(setting_key)
WHERE t.type = 'LIBRARY'
   OR t.id IN (SELECT u.tenant_id FROM users u JOIN user_roles r ON r.user_id = u.id AND r.role_code = 'ADMIN');
