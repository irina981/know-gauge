-- The backend says "tenant" where the UI says "workspace": the invitations of V6 become tenant_invitations, with
-- their key, identity sequence, constraints and indexes renamed to match. The rows stay as they are.
ALTER TABLE workspace_invitations RENAME TO tenant_invitations;

ALTER TABLE tenant_invitations RENAME CONSTRAINT workspace_invitations_pkey TO tenant_invitations_pkey;
ALTER SEQUENCE workspace_invitations_id_seq RENAME TO tenant_invitations_id_seq;

ALTER TABLE tenant_invitations RENAME CONSTRAINT fk_workspace_invitations_tenant TO fk_tenant_invitations_tenant;
ALTER TABLE tenant_invitations RENAME CONSTRAINT fk_workspace_invitations_role TO fk_tenant_invitations_role;
ALTER TABLE tenant_invitations RENAME CONSTRAINT fk_workspace_invitations_user TO fk_tenant_invitations_user;
ALTER TABLE tenant_invitations RENAME CONSTRAINT uk_workspace_invitations_token TO uk_tenant_invitations_token;
ALTER TABLE tenant_invitations RENAME CONSTRAINT uk_workspace_invitations_tenant_email
    TO uk_tenant_invitations_tenant_email;

ALTER INDEX idx_workspace_invitations_tenant_id RENAME TO idx_tenant_invitations_tenant_id;
ALTER INDEX idx_workspace_invitations_pending_email RENAME TO idx_tenant_invitations_pending_email;
