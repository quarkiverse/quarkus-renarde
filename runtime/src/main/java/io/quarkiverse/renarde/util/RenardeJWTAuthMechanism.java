package io.quarkiverse.renarde.util;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.renarde.impl.RenardeConfig;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.quarkus.smallrye.jwt.runtime.auth.SmallRyeJwtConfig;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * This class replaces the original JWTAuthMechanism with a lower priority, so that other interactive auth
 * such as WebAuthn, OIDC or Form get a chance to issue a redirect challenge, when this dumbass can only
 * issue a 401, and since the auth mechanisms are in random order, we'd sometimes get a 401 and sometimes
 * a proper redirect. It now issues a redirect, but stays in lower priority.
 */
@Priority(2000)
@ApplicationScoped
public class RenardeJWTAuthMechanism extends JWTAuthMechanism {

    @Inject
    RenardeConfig config;

    // FIXME: make it configurable
    String locationCookie = "quarkus-redirect-location";

    // for CDI proxy
    RenardeJWTAuthMechanism() {
        this(null);
    }

    public RenardeJWTAuthMechanism(SmallRyeJwtConfig config) {
        super(config);
    }

    @Override
    public int getPriority() {
        return -1000;
    }

    protected void storeInitialLocation(final RoutingContext exchange) {
        exchange.response().addCookie(Cookie.cookie(locationCookie, exchange.request().absoluteURI())
                .setPath("/").setSecure(exchange.request().isSSL()));
    }

    static Uni<ChallengeData> getRedirect(final RoutingContext exchange, final String location) {
        String loc = exchange.request().scheme() + "://" + exchange.request().host() + location;
        return Uni.createFrom().item(new ChallengeData(302, "Location", loc));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (config.getLoginPage() != null) {
            // we need to store the URL
            storeInitialLocation(context);
            return getRedirect(context, config.getLoginPage());
        } else {
            return super.getChallenge(context);
        }
    }
}
