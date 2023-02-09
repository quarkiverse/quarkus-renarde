package io.quarkiverse.renarde.security.impl;

import java.util.Collections;
import java.util.Set;

import io.quarkiverse.renarde.security.RenardeTenantProvider;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DefaultBean
public class DefaultTenantProvider implements RenardeTenantProvider {

    @Override
    public String getTenantId() {
        return null;
    }

    @Override
    public Set<String> getTenants() {
        return Collections.emptySet();
    }

}
