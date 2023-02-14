package io.quarkiverse.renarde.security;

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

import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
public class RenardeSecurity {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String jwtIssuer;
    @ConfigProperty(name = "mp.jwt.token.cookie")
    String jwtCookie;

    public NewCookie makeUserCookie(RenardeUser user) {
        Set<String> roles = user.roles();
        String token = Jwt.issuer(jwtIssuer)
                .upn(user.userId())
                .groups(roles)
                // FIXME: config
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        // FIXME: expiry, auto-refresh?
        return new NewCookie(jwtCookie, token, "/", null, Cookie.DEFAULT_VERSION, null, NewCookie.DEFAULT_MAX_AGE, null, false,
        		true);
    }

    @Inject
    SecurityIdentity identity;

    @Inject
    RenardeTenantProvider tenantProvider;

    @Inject
    RenardeUserProvider userProvider;

    @Inject
    Flash flash;

    public RenardeUser getUser() {
        if (!identity.isAnonymous()) {
            String name = identity.getPrincipal().getName();
            String tenantId = tenantProvider.getTenantId();
            if (tenantId == null) {
                tenantId = "manual";
            }
            RenardeUser user = userProvider.findUser(tenantId, name);
            // old cookie, no such user
            if (user == null) {
                flash.flash("message", "Invalid user: " + name);
                throw new RedirectException(makeLogoutResponse());
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

    public Response makeLogoutResponse() {
        Set<String> tenants = tenantProvider.getTenants();
        List<NewCookie> cookies = new ArrayList<>(tenants.size() + 1);
        // Default tenant
        cookies.add(new NewCookie("q_session", null, "/", null, null, 0, false, true));
        // Named tenants
        for (String tenant : tenants) {
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
}
