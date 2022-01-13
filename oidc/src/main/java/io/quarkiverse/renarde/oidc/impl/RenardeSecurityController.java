package io.quarkiverse.renarde.oidc.impl;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.oidc.RenardeOidcHandler;
import io.quarkiverse.renarde.oidc.RenardeSecurity;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.Authenticated;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;

// Note that we're using _renarde because MS doesn't allow @renarde, so
// don't try to be smart here
@Path("_renarde/security")
public class RenardeSecurityController extends Controller {

    /**
     * Auth trigger for OIDC
     */
    @Path("login-{provider}")
    @Authenticated
    public void loginUsingOidc(@RestPath String provider) {
        // this can be called if we're authenticated on OIDC but the user didn't go through
        // with completion
        JWTCallerPrincipal principal = (JWTCallerPrincipal) identity.getPrincipal();
        String tenantId = oidcSession.getTenantId();
        if (tenantId == null) {
            tenantId = "manual";
        }
        oidcHandler.loginWithOidcSession(tenantId, principal.getName());
    }

    /**
     * Logout action, redirects to index
     */
    @Path("logout")
    public Response logout() {
        // FIXME: doesn't work
        // oidcSession.logout().await().indefinitely();
        return security.makeLogoutResponse();
    }

    @Inject
    RenardeSecurity security;

    @Inject
    RenardeOidcHandler oidcHandler;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    UserInfo userInfo;

    @Inject
    OidcSession oidcSession;

    @Authenticated
    @Path("github-success")
    public void githubSuccess() {
        // something is coming
        String authId = userInfo.getLong("id").toString();
        oidcHandler.oidcSuccess(oidcSession.getTenantId(), authId);
    }

    @Authenticated
    @Path("facebook-success")
    public void facebookSuccess() {
        // something is coming
        String authId = userInfo.getString("id");
        oidcHandler.oidcSuccess(oidcSession.getTenantId(), authId);
    }

    // for every provider
    @Authenticated
    @Path("oidc-success")
    public void oidcSuccessGet() {
        oidcLoginSuccess();
    }

    // for Apple, which needs this as well as the GET endpoint
    @Authenticated
    @Path("oidc-success")
    @POST
    public void oidcSuccessPost() {
        oidcLoginSuccess();
    }

    private void oidcLoginSuccess() {
        String authId = idToken.getName();
        oidcHandler.oidcSuccess(oidcSession.getTenantId(), authId);
    }
}
