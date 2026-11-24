-- Which owners may have a value of each setting (ADR014), decided here and not in the code: SYSTEM (the System
-- settings page; overrides the properties files) and/or TENANT (a workspace's own value, Workspaces -> Limits). The
-- application reads the owners and the type from setting_configs at startup; the code only describes each key
-- (label, validation, the properties value it reads and writes). More owners (e.g. a billing plan) are new rows.
CREATE TABLE setting_config_owners (
    setting_key VARCHAR(100) NOT NULL,
    setting_owner VARCHAR(20) NOT NULL,

    CONSTRAINT pk_setting_config_owners PRIMARY KEY (setting_key, setting_owner),
    CONSTRAINT fk_setting_config_owners_config FOREIGN KEY (setting_key)
        REFERENCES setting_configs(setting_key) ON DELETE CASCADE,
    CONSTRAINT chk_setting_config_owners_owner CHECK (setting_owner IN ('SYSTEM', 'TENANT'))
);

-- Every setting so far has a system value.
INSERT INTO setting_config_owners (setting_key, setting_owner)
SELECT setting_key, 'SYSTEM' FROM setting_configs;

-- The workspace limits may also have a value per workspace (inserted only for keys that exist).
INSERT INTO setting_config_owners (setting_key, setting_owner)
SELECT setting_key, 'TENANT' FROM setting_configs
WHERE setting_key IN ('kg.settings.tenant.monthly-token-limit',
                      'kg.settings.tenant.storage-limit-mb',
                      'kg.settings.tenant.concurrent-generations-per-user',
                      'kg.settings.tenant.participants-per-test');

-- Values of an owner a key does not allow were ignored already; they cannot reference setting_config_owners.
DELETE FROM settings s
WHERE NOT EXISTS (SELECT 1 FROM setting_config_owners o
                  WHERE o.setting_key = s.setting_key AND o.setting_owner = s.setting_owner);

-- A value may only be stored for an owner its key allows. This also covers the key itself, so the simple foreign key
-- to setting_configs (V7) goes.
ALTER TABLE settings DROP CONSTRAINT fk_settings_config;
ALTER TABLE settings
    ADD CONSTRAINT fk_settings_config_owner FOREIGN KEY (setting_key, setting_owner)
        REFERENCES setting_config_owners(setting_key, setting_owner);
