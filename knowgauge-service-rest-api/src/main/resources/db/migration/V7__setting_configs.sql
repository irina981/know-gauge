-- Every possible setting key in one table, with its type (CONFIG: a configuration value such as a default of new
-- tests; LIMIT: a limit on what a workspace or user may use). settings.setting_key references it. The keys and types
-- must match the SettingDefinitions in the code (CoreSystemSettings, other SettingsContributors); startup checks
-- that. A new setting needs a new migration that inserts its key here.
CREATE TABLE setting_configs (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_type VARCHAR(20) NOT NULL,

    CONSTRAINT chk_setting_configs_type CHECK (setting_type IN ('CONFIG', 'LIMIT'))
);

INSERT INTO setting_configs (setting_key, setting_type) VALUES
    ('kg.settings.tenant.monthly-token-limit',              'LIMIT'),
    ('kg.settings.tenant.storage-limit-mb',                 'LIMIT'),
    ('kg.settings.tenant.concurrent-generations-per-user',  'LIMIT'),
    ('kg.testgen.defaults.generation-model',                'CONFIG'),
    ('kg.testgen.verification.chat-model.openai.model',     'CONFIG'),
    ('kg.testgen.defaults.question-generation-batch-size',  'CONFIG'),
    ('kg.testgen.defaults.question-count',                  'CONFIG'),
    ('kg.testgen.defaults.difficulty',                      'CONFIG'),
    ('kg.invitations.max-per-request',                      'LIMIT'),
    ('kg.ingestion.job.batch-size',                         'CONFIG');

-- Values of keys no longer defined were ignored already; they cannot reference setting_configs.
DELETE FROM settings WHERE setting_key NOT IN (SELECT setting_key FROM setting_configs);

ALTER TABLE settings
    ADD CONSTRAINT fk_settings_config FOREIGN KEY (setting_key) REFERENCES setting_configs(setting_key);

-- A setting belongs to an owner: the system, or a tenant (owner_id is the tenant id). More owners may follow, e.g. a
-- billing plan with settings of its own, so owner_id cannot reference tenants any more.
ALTER TABLE settings RENAME COLUMN setting_type TO setting_owner;
ALTER TABLE settings RENAME COLUMN tenant_id TO owner_id;
ALTER TABLE settings RENAME CONSTRAINT chk_settings_type TO chk_settings_owner;
ALTER TABLE settings RENAME CONSTRAINT chk_settings_tenant TO chk_settings_owner_id;
ALTER TABLE settings DROP CONSTRAINT fk_settings_tenant;
-- uq_settings_key (setting_owner, owner_id, setting_key) follows the renamed columns.
DROP INDEX idx_settings_tenant_id;
CREATE INDEX idx_settings_owner ON settings(setting_owner, owner_id);

-- In place of the foreign key's ON DELETE CASCADE: a deleted tenant takes its settings with it.
CREATE FUNCTION delete_tenant_settings() RETURNS trigger AS $$
BEGIN
    DELETE FROM settings WHERE setting_owner = 'TENANT' AND owner_id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tenants_delete_settings
    AFTER DELETE ON tenants
    FOR EACH ROW EXECUTE FUNCTION delete_tenant_settings();
