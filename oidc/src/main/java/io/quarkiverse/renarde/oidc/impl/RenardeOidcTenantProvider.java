package io.quarkiverse.renarde.oidc.impl;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.renarde.security.RenardeTenantProvider;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.runtime.OidcConfig;

@ApplicationScoped
public class RenardeOidcTenantProvider implements RenardeTenantProvider {

    @Inject
    OidcSession oidcSession;

    @Inject
    OidcConfig oidcConfig;

    @Override
    public String getTenantId() {
        return oidcSession.getTenantId();
    }

    @Override
    public Set<String> getTenants() {
        return oidcConfig.namedTenants.keySet();
    }
}
