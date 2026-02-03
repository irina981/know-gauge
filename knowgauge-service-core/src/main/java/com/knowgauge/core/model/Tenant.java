package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.TenantType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
class Tenant extends AuditableObject{
    Long id;
    TenantType type;

    String name;        // "Irina", "ACME Corp"
    String ownerUserId;
}