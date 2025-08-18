package io.quarkiverse.renarde.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkiverse.renarde.configuration.RenardeConfig;
import io.quarkiverse.renarde.impl.RenardeConfigBean;
import io.quarkus.qute.TemplateGlobal;
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

    private static final Logger log = Logger.getLogger(RenardeJWTAuthMechanism.class);

    @TemplateGlobal
    public final static String REDIRECT_URI = "redirect_uri";

    @Inject
    RenardeConfigBean config;

    @ConfigProperty(name = "quarkus.renarde.auth.redirect.cookie")
    String locationCookie;

    @ConfigProperty(name = "quarkus.renarde.auth.redirect.type")
    RenardeConfig.RenardeAuthConfig.Redirect.Type redirectType;

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
        // always redirect to the login page, except when we have a special REST header
        String authHeader = context.request().headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null) {
            return super.getChallenge(context);
        }

        if (context.request().uri().equals(config.getLoginPage())) {
            log.errorf(
                    "Avoiding redirect loop, make sure that your endpoint annotated with @LoginPage is accessible without being authenticated: %s",
                    config.getLoginPage());
            return super.getChallenge(context);
        } else {
            String redirectUri;
            if (redirectType == RenardeConfig.RenardeAuthConfig.Redirect.Type.cookie) {
                // we need to store the URL
                storeInitialLocation(context);
                redirectUri = config.getLoginPage();
            } else {
                redirectUri = "%s?%s=%s".formatted(config.getLoginPage(), RenardeJWTAuthMechanism.REDIRECT_URI,
                        URLEncoder.encode(context.request().absoluteURI(), StandardCharsets.UTF_8));
            }
            return getRedirect(context, redirectUri);
        }
    }
}
