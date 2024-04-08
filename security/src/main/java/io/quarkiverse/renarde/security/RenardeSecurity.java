package io.quarkiverse.renarde.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.NewCookie.SameSite;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkiverse.renarde.configuration.RenardeConfig;
import io.quarkiverse.renarde.impl.RenardeConfigBean;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@ApplicationScoped
public class RenardeSecurity {

    private final static Logger log = Logger.getLogger(RenardeSecurity.class);

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String jwtIssuer;
    @ConfigProperty(name = "mp.jwt.token.cookie")
    String jwtCookie;

    @ConfigProperty(name = "quarkus.oidc.authentication.cookie-suffix", defaultValue = "q_session")
    String oidcCookie;

    @ConfigProperty(name = "quarkus.renarde.auth.location-cookie")
    String locationCookie;

    @Inject
    HttpServerRequest request;

    @Inject
    HttpServerResponse response;

    @Inject
    RenardeConfigBean config;

    @Inject
    SecurityIdentity identity;

    @Inject
    RenardeTenantProvider tenantProvider;

    @Inject
    RenardeUserProvider userProvider;

    @Inject
    RenardeConfig renardeConfig;

    @Inject
    HttpHeaders headers;

    @Inject
    Flash flash;

    public NewCookie makeUserCookie(RenardeUser user) {
        Set<String> roles = user.roles();
        String token = Jwt.issuer(jwtIssuer)
                .upn(user.userId())
                .groups(roles)
                // FIXME: config
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        // FIXME: expiry, auto-refresh?
        return new NewCookie.Builder(jwtCookie)
                .value(token)
                .path("/")
                .sameSite(SameSite.LAX)
                .httpOnly(true)
                .build();
    }

    public RenardeUser getUser() {
        if (!identity.isAnonymous()) {
            String authId = getUserId(identity.getPrincipal());
            String tenantId = tenantProvider.getTenantId();
            if (tenantId == null) {
                tenantId = "manual";
            }
            RenardeUser user = userProvider.findUser(tenantId, authId);
            // old cookie, no such user
            if (user == null) {
                redirectToLogin("Invalid user: " + authId);
            }
            // let's not produce users if we're still registering them, but we must differentiate them
            // from inexistent users, for which we're triggering a logout, because during registration
            // we have an OIDC session but the user is not registered yet, so we don't want there to
            // be a current user, but we also don't want to logout and clear the OIDC session while
            // we're registering the user
            if (!user.registered()) {
                return null;
            }
            return user;
        }
        return null;
    }

    public static String getUserId(Principal principal) {
        if (principal instanceof JsonWebToken) {
            // we cannot trust its getName() which is not unique
            JsonWebToken idToken = (JsonWebToken) principal;
            String authId = idToken.getClaim(Claims.upn.name());
            // DO NOT use preferred_username which is not unique
            if (authId == null) {
                authId = idToken.getClaim(Claims.sub.name());
            }
            return authId;
        } else {
            // most others should be unique
            return principal.getName();
        }
    }

    public Response makeLogoutResponse() {
        try {
            return this.makeLogoutResponse(new URI("/"));
        } catch (URISyntaxException e) {
            // can't happen
            throw new RuntimeException(e);
        }
    }

    public Response makeLogoutResponse(URI redirectUri) {
        return Response.seeOther(redirectUri).cookie(makeLogoutCookies()).build();
    }

    public NewCookie[] makeLogoutCookies() {
        Set<String> tenants = tenantProvider.getTenants();
        List<NewCookie> cookies = new ArrayList<>(tenants.size() + 1);
        // Default tenant
        cookies.add(invalidateCookie(oidcCookie));

        // Named tenants
        for (String tenant : tenants) {
            cookies.add(invalidateCookie(oidcCookie + "_" + tenant));
        }
        // Manual
        cookies.add(invalidateCookie(jwtCookie));

        return cookies.toArray(new NewCookie[0]);
    }

    public Response makeRedirectAfterLogin(URI uri) {
        Map<String, jakarta.ws.rs.core.Cookie> cookies = headers.getCookies();
        jakarta.ws.rs.core.Cookie redirectCookie = cookies.get(renardeConfig.auth.locationCookie);
        NewCookie newCookie = null;
        if (redirectCookie != null) {
            // consume it
            String value = redirectCookie.getValue();
            newCookie = invalidateCookie(renardeConfig.auth.locationCookie);
            uri = URI.create(value);
        }
        ResponseBuilder response = Response.seeOther(uri);
        if (newCookie != null) {
            response.cookie(newCookie);
        }
        return response.build();
    }

    public void redirectAfterLogin(URI uri) {
        throw new RedirectException(makeRedirectAfterLogin(uri));
    }

    public Response makeRedirectToLogin(String message) {
        if (request.uri().equals(config.getLoginPage())) {
            // this would cause a redirect loop, not sure how to handle, but not by redirecting
            log.errorf("Redirect loop at %s, giving up on clearing bad JWT cookie", config.getLoginPage());
            return Response.serverError().build();
        }
        // see https://github.com/quarkiverse/quarkus-renarde/issues/194
        if (response.headers().contains(HttpHeaders.LOCATION)) {
            // workaround bug where auth challenge sets location header, and we add one, resulting
            // in there being two, which is invalid HTTP
            response.headers().remove(HttpHeaders.LOCATION);
        }
        ResponseBuilder builder = Response.seeOther(URI.create(config.getLoginPage()));
        builder.cookie(makeLogoutCookies());
        builder.cookie(saveURICookie());

        flash.flash("message", message);

        return builder.build();
    }

    public void redirectToLogin(String message) {
        throw new RedirectException(makeRedirectToLogin(message));
    }

    private NewCookie saveURICookie() {
        return new NewCookie.Builder(locationCookie).path("/").value(request.absoluteURI()).secure(request.isSSL()).build();
    }

    public NewCookie invalidateCookie(String cookieName) {
        return new NewCookie.Builder(cookieName).path("/").maxAge(0).build();
    }

    public boolean hasUserCookie() {
        return request.getCookie(jwtCookie) != null;
    }

}
