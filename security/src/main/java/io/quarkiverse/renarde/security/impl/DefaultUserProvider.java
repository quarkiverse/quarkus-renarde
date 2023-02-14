package io.quarkiverse.renarde.security.impl;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkus.arc.DefaultBean;
import io.quarkus.logging.Log;

@ApplicationScoped
@DefaultBean
public class DefaultUserProvider implements RenardeUserProvider {

    @Override
    public RenardeUser findUser(String tenantId, String authId) {
        Log.errorf("No bean implementing RenardeUserProvider declared: cannot find user %s/%s", tenantId, authId);
        return null;
    }

}
