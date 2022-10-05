package io.quarkiverse.renarde.security;

public interface RenardeUserProvider {
    RenardeUser findUser(String tenantId, String authId);
}
