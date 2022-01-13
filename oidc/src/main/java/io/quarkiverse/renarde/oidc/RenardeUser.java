package io.quarkiverse.renarde.oidc;

import java.util.Set;

public interface RenardeUser {

    Set<String> getRoles();

    String getUserId();

    boolean isRegistered();
}
