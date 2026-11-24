-- Who may belong to a tenant (ADR010): a LIBRARY has no users, a personal (INDIVIDUAL) tenant only its owner.
-- ORGANIZATION tenants (later) may have several users.
--
-- Both rules span two tables, so they are constraint triggers rather than CHECKs. They are deferred to the end of the
-- transaction: sign-in creates the tenant, then its user, then sets the tenant's owner, all in one transaction.

CREATE FUNCTION check_tenant_membership() RETURNS trigger
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
    IF tenant_type = 'INDIVIDUAL' AND EXISTS (SELECT 1 FROM users u WHERE u.tenant_id = checked_tenant_id
                                                  AND u.id IS DISTINCT FROM tenant_owner) THEN
        RAISE EXCEPTION 'Tenant % is personal: only its owner (user %) may belong to it', checked_tenant_id,
            tenant_owner USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END
$$;

-- A user joining or moving to a tenant.
CREATE CONSTRAINT TRIGGER trg_users_tenant_membership
    AFTER INSERT OR UPDATE OF tenant_id ON users
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_tenant_membership();

-- A tenant changing its type or owner (a deleted owner sets owner_user_id to NULL, see fk_tenants_owner).
CREATE CONSTRAINT TRIGGER trg_tenants_membership
    AFTER INSERT OR UPDATE OF type, owner_user_id ON tenants
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_tenant_membership();

-- The rows already there must follow the rules too.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users u JOIN tenants t ON t.id = u.tenant_id
               WHERE t.type = 'LIBRARY'
                  OR (t.type = 'INDIVIDUAL' AND u.id IS DISTINCT FROM t.owner_user_id)) THEN
        RAISE EXCEPTION 'Users break the tenant membership rules: a library has users, or a personal tenant has a'
            ' user other than its owner';
    END IF;
END
$$;
