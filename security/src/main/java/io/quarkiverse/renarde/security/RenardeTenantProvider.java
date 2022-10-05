package io.quarkiverse.renarde.security;

import java.util.Set;

/**
 * To be implemented by OIDC for providing a tenant ID
 */
public interface RenardeTenantProvider {
    public String getTenantId();

    public Set<String> getTenants();
}
