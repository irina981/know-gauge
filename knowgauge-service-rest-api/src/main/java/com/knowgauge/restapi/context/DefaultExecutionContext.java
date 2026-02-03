package com.knowgauge.restapi.context;

import java.util.OptionalLong;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentTenantProvider;
import com.knowgauge.core.context.CurrentUserProvider;
import com.knowgauge.core.context.ExecutionContext;

@Component
public class DefaultExecutionContext implements ExecutionContext {

    private final CurrentTenantProvider tenantProvider;
    private final CurrentUserProvider userProvider;

    public DefaultExecutionContext(CurrentTenantProvider tenantProvider,
                                   CurrentUserProvider userProvider) {
        this.tenantProvider = tenantProvider;
        this.userProvider = userProvider;
    }

    @Override
    public long tenantId() {
        return tenantProvider.tenantId();
    }

    @Override
    public OptionalLong userId() {
        return userProvider.userId();
    }
}
