package io.quarkiverse.renarde.oidc;

public interface RenardeOidcHandler {

    void oidcSuccess(String tenantId, String authId);

    void loginWithOidcSession(String tenantId, String authId);

}
