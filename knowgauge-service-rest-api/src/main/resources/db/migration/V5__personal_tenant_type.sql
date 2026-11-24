-- A user's own tenant has the type PERSONAL (was INDIVIDUAL), the name the UI uses too.

-- The membership rules of V4__tenant_membership_rules.sql with the new name: a LIBRARY has no users, a PERSONAL
-- tenant only its owner.
CREATE OR REPLACE FUNCTION check_tenant_membership() RETURNS trigger
LANGUAGE plpgsql
SET search_path FROM CURRENT
AS $$
DECLARE
    checked_tenant_id BIGINT;
    tenant_type VARCHAR(30);
    tenant_owner BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'users' THEN
        checked_tenant_id := NEW.tenant_id;
    ELSE
        checked_tenant_id := NEW.id;
    END IF;
    IF checked_tenant_id IS NULL THEN
        RETURN NULL; -- the system user has no tenant
    END IF;

    SELECT t.type, t.owner_user_id INTO tenant_type, tenant_owner FROM tenants t WHERE t.id = checked_tenant_id;

    IF tenant_type = 'LIBRARY' AND EXISTS (SELECT 1 FROM users u WHERE u.tenant_id = checked_tenant_id) THEN
        RAISE EXCEPTION 'Tenant % is a library and cannot have users', checked_tenant_id
            USING ERRCODE = 'check_violation';
    END IF;
    IF tenant_type = 'PERSONAL' AND EXISTS (SELECT 1 FROM users u WHERE u.tenant_id = checked_tenant_id
                                                AND u.id IS DISTINCT FROM tenant_owner) THEN
        RAISE EXCEPTION 'Tenant % is personal: only its owner (user %) may belong to it', checked_tenant_id,
            tenant_owner USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END
$$;

-- Check the membership rules at once in this migration: Postgres does not alter a table with deferred trigger events
-- pending, and the UPDATE queues them.
SET CONSTRAINTS ALL IMMEDIATE;

ALTER TABLE tenants DROP CONSTRAINT chk_tenants_type;
UPDATE tenants SET type = 'PERSONAL' WHERE type = 'INDIVIDUAL';
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_type CHECK (type IN ('PERSONAL', 'ORGANIZATION', 'LIBRARY'));
