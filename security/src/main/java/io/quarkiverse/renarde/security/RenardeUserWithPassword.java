package io.quarkiverse.renarde.security;

public interface RenardeUserWithPassword extends RenardeUser {
    // we don't want getters because it messes with Hibernate getters in case of type mismatch
    String password();
}
