package io.quarkiverse.renarde.oidc;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.renarde.oidc.impl.RenardeGithubClient;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
public class RenardeSecurity {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String jwtIssuer;
    @ConfigProperty(name = "mp.jwt.token.cookie")
    String jwtCookie;

    public NewCookie makeUserCookie(RenardeUser user) {
        Set<String> roles = user.getRoles();
        String token = Jwt.issuer(jwtIssuer)
                .upn(user.getUserId())
                .groups(roles)
                // FIXME: config
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        // FIXME: expiry, auto-refresh?
        return new NewCookie(jwtCookie, token, "/", null, Cookie.DEFAULT_VERSION, null, NewCookie.DEFAULT_MAX_AGE, null, false,
                false);
    }

    @Inject
    SecurityIdentity identity;

    @Inject
    OidcSession oidcSession;

    @Inject
    RenardeUserProvider userProvider;

    @Inject
    OidcConfig oidcConfig;

    @Inject
    Flash flash;

    public RenardeUser getUser() {
        if (!identity.isAnonymous()) {
            JWTCallerPrincipal principal = (JWTCallerPrincipal) identity.getPrincipal();
            String tenantId = oidcSession.getTenantId();
            if (tenantId == null) {
                tenantId = "manual";
            }
            RenardeUser user = userProvider.findUser(tenantId, principal.getName());
            // old cookie, no such user
            if (user == null) {
                flash.flash("message", "Invalid user: " + principal.getName());
                throw new RedirectException(makeLogoutResponse());
            }
            // let's not produce users if we're still registering them, but we must differentiate them
            // from inexistent users, for which we're triggering a logout, because during registration
            // we have an OIDC session but the user is not registered yet, so we don't want there to
            // be a current user, but we also don't want to logout and clear the OIDC session while
            // we're registering the user
            if (!user.isRegistered()) {
                return null;
            }
            return user;
        }
        return null;
    }

    public Response makeLogoutResponse() {
        List<NewCookie> cookies = new ArrayList<>(oidcConfig.namedTenants.size() + 1);
        // Default tenant
        cookies.add(new NewCookie("q_session", null, "/", null, null, 0, false, true));
        // Named tenants
        for (String tenant : oidcConfig.namedTenants.keySet()) {
            cookies.add(new NewCookie("q_session_" + tenant, null, "/", null, null, 0, false, true));
        }
        // Manual
        cookies.add(new NewCookie(jwtCookie, null, "/", null, null, 0, false, true));
        try {
            return Response.seeOther(new URI("/")).cookie(cookies.toArray(new NewCookie[0])).build();
        } catch (URISyntaxException e) {
            // can't happen
            throw new RuntimeException(e);
        }
    }

    @RestClient
    RenardeGithubClient client;

    @Inject
    AccessTokenCredential accessToken;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    UserInfo userInfo;

    public String getOidcEmail() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                List<RenardeGithubClient.Email> emails = client.getEmails("Bearer " + accessToken.getToken());
                for (RenardeGithubClient.Email emailStruct : emails) {
                    if (emailStruct.primary) {
                        return emailStruct.email;
                    }
                }
                return null;
            default:
                return idToken.getClaim(Claims.email);
        }
    }

    public String getOidcFirstName() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                return firstPart(userInfo.getString("name"));
            case "microsoft":
                return firstPart(idToken.getClaim("name"));
            default:
                return idToken.getClaim(Claims.given_name);
        }
    }

    private String firstPart(String name) {
        if (name == null)
            return null;
        int firstSpace = name.indexOf(' ');
        if (firstSpace != -1) {
            return name.substring(0, firstSpace);
        } else {
            return name;
        }
    }

    private String secondPart(String name) {
        if (name == null)
            return null;
        int firstSpace = name.indexOf(' ');
        if (firstSpace != -1) {
            return name.substring(firstSpace + 1);
        } else {
            return null;
        }
    }

    public String getOidcLastName() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                return secondPart(userInfo.getString("name"));
            case "microsoft":
                return secondPart(idToken.getClaim("name"));
            default:
                return idToken.getClaim(Claims.family_name);
        }
    }

    public String getOidcUserName() {
        String tenantId = oidcSession.getTenantId();
        switch (tenantId) {
            case "github":
                return userInfo.getString("login");
            case "facebook":
                return null;
            default:
                return idToken.getClaim(Claims.preferred_username);
        }
    }
}
