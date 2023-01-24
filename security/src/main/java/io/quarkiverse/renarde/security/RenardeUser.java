package io.quarkiverse.renarde.security;

import java.util.Set;

public interface RenardeUser {

    // we don't want getters because it messes with Hibernate getters in case of type mismatch
    Set<String> roles();

    // we don't want getters because it messes with Hibernate getters in case of type mismatch
    String userId();

    // we don't want getters because it messes with Hibernate getters in case of type mismatch
    boolean registered();
}
