-- Authorization lives in KnowGauge, not in the identity provider (ADR012): Keycloak only authenticates.
-- roles lists every role the app knows (the Role enum); user_roles holds each user's roles, so a user can have
-- several, e.g. ADMIN and TENANT_ADMIN of their own tenant. Tenant roles apply in the user's one tenant.
CREATE TABLE roles (
    code VARCHAR(30) PRIMARY KEY,
    description VARCHAR(300) NOT NULL
);

INSERT INTO roles (code, description) VALUES
    ('ADMIN', 'Application admin, across tenants: changes the shared library, admin API and actuator'),
    ('TENANT_ADMIN', 'Manages everything in their own tenant'),
    ('TENANT_USER', 'Runs tests in a tenant someone else administers (not assigned yet)'),
    ('TENANT_VIEWER', 'Reads a tenant''s content, tests and results, changes nothing (not assigned yet)');

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(30) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles(code)
);

CREATE INDEX idx_user_roles_role_code ON user_roles(role_code);

-- The tenant role each user had so far moves over; the column goes.
INSERT INTO user_roles (user_id, role_code)
SELECT id, role FROM users WHERE role IS NOT NULL;

ALTER TABLE users DROP CONSTRAINT chk_users_tenant_and_role;
ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users DROP COLUMN role;
ALTER TABLE users ADD CONSTRAINT chk_users_tenant CHECK (id = 0 OR tenant_id IS NOT NULL);
