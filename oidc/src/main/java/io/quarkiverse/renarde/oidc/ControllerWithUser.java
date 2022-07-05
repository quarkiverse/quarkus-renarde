package io.quarkiverse.renarde.oidc;

import javax.inject.Inject;

import io.quarkiverse.renarde.Controller;

/**
 * A controller subtype with a current user.
 *
 * @param <U> your implementation of {@link RenardeUser}
 */
public abstract class ControllerWithUser<U extends RenardeUser> extends Controller {
    @Inject
    protected RenardeSecurity security;

    @SuppressWarnings("unchecked")
    /**
     * Obtains the currently logged in user, if any.
     *
     * @return the currently logged in user, or null.
     */
    protected U getUser() {
        return (U) security.getUser();
    }
}
