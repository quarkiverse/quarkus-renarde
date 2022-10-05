package io.quarkiverse.renarde.security;

import java.util.Set;

public interface RenardeUser {

    Set<String> getRoles();

    String getUserId();

    boolean isRegistered();
}
