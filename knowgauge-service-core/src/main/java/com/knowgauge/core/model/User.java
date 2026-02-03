package com.knowgauge.core.model;

import java.util.Map;

import com.knowgauge.core.model.enums.Role;

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
public class User extends AuditableObject {

    private String externalId;   // Keycloak sub (ID)

    private Long tenantId;

    private Role role;

    private Map<String, String> preferences;

    private String displayName;
    
    private String email;
}


