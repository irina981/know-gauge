package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.TenantType;

class Tenant {
    Long id;
    TenantType type;

    String name;        // "Irina", "ACME Corp"
    String ownerUserId;
}