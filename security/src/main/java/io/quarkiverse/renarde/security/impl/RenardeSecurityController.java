package io.quarkiverse.renarde.security.impl;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.RenardeSecurity;

// Note that we're using _renarde because MS doesn't allow @renarde, so
// don't try to be smart here
@Path("_renarde/security")
public class RenardeSecurityController extends Controller {

    @Inject
    RenardeSecurity security;

    /**
     * Logout action, redirects to index
     */
    @Path("logout")
    public Response logout() {
        // FIXME: doesn't work
        // oidcSession.logout().await().indefinitely();
        return security.makeLogoutResponse();
    }
}
