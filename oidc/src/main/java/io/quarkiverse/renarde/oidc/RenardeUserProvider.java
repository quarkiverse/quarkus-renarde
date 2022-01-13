package io.quarkiverse.renarde.oidc;

public interface RenardeUserProvider {
    RenardeUser findUser(String tenantId, String authId);
}
