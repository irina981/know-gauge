package com.knowgauge.restapi.context;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentTenantProvider;

@Component
@Profile({"dev", "docker"})
public class DefaultTenantProvider implements CurrentTenantProvider {
    @Override public long tenantId() { return 1L; }
}

