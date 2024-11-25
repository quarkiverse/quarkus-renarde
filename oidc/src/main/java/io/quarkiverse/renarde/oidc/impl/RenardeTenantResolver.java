package io.quarkiverse.renarde.oidc.impl;

import java.util.Map.Entry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class RenardeTenantResolver implements TenantResolver {

    @Inject
    OidcConfig oidcConfig;

    @ConfigProperty(name = "mp.jwt.token.cookie")
    String jwtCookie;

    @Override
    public String resolve(RoutingContext context) {
        String knownTenant = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (knownTenant != null)
            return knownTenant;
        // Named tenants
        for (Entry<String, OidcTenantConfig> tenantEntry : oidcConfig.namedTenants().entrySet()) {
            if (!tenantEntry.getValue().tenantEnabled())
                continue;
            String tenant = tenantEntry.getKey();
            // First case: login
            // Note that Router.getURI only works in JAX-RS endpoints
            if (context.request().path().equals("/_renarde/security/login-" + tenant)) {
                return tenant;
            }
        }

        // manual JWT session
        Cookie cookie = context.request().getCookie(jwtCookie);
        if (cookie != null) {
            return "manual";
        }

        // Not logged in or default tenant
        return null;
    }
}
