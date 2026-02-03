package com.knowgauge.core.model.enums;

public enum Role {

    APPLICATION_ADMIN,   // global (can also be stored in Keycloak realm roles)

    TENANT_ADMIN,        // manages content in tenant

    TENANT_USER          // runs tests
}

