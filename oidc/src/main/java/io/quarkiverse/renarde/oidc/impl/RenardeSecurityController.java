package io.quarkiverse.renarde.oidc.impl;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.oidc.RenardeOidcHandler;
import io.quarkiverse.renarde.oidc.RenardeOidcSecurity;
import io.quarkiverse.renarde.security.RenardeSecurity;
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
        oidcHandler.loginWithOidcSession(tenantId, RenardeSecurity.getUserId(principal));
    }

    @Inject
    RenardeOidcSecurity security;

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
        String authId = userInfo.getLong("id").toString();
        oidcHandler.oidcSuccess(oidcSession.getTenantId(), authId);
    }

    @Authenticated
    @Path("twitter-success")
    public void twitterSuccess() {
        String authId = userInfo.getObject("data").getString("id");
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
        String authId = RenardeSecurity.getUserId(idToken);
        oidcHandler.oidcSuccess(oidcSession.getTenantId(), authId);
    }
}
