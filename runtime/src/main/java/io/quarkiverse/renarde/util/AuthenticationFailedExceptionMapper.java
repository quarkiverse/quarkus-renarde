package io.quarkiverse.renarde.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.crypto.AEADBadTagException;

import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationFailedExceptionMapper {

    // FIXME: we can do better than this filter
    @RouteFilter(100)
    void myFilter(RoutingContext rc) {
        rc.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new BiConsumer<RoutingContext, Throwable>() {
            @Override
            public void accept(RoutingContext routingContext, Throwable throwable) {
                while (throwable.getCause() != null)
                    throwable = throwable.getCause();
                if (throwable instanceof InvalidJwtException) {
                    InvalidJwtException x = (InvalidJwtException) throwable;
                    if (x.hasErrorCode(ErrorCodes.EXPIRED)) {
                        redirectToRoot(routingContext, "Login expired, you've been logged out");
                        return;
                    }
                }
                // This happens when the private/public keys change, like in DEV mode
                if (throwable instanceof AEADBadTagException) {
                    redirectToRoot(routingContext, "Something is rotten about your JWT, clearing it");
                    return;
                }
                if (throwable instanceof AuthenticationRedirectException) {
                    // handled upstream
                    return;
                }
                // FIXME: what now?
            }

            private void redirectToRoot(RoutingContext routingContext, String message) {
                // FIXME: constant
                routingContext.removeCookie("QuarkusUser").setPath("/");
                Map<String, Object> map = new HashMap<>();
                // FIXME: format?
                map.put("message", message);
                Flash.setFlashCookie(routingContext.response(), map);
                // FIXME: URI, perhaps redirect to login page?
                // Note that this calls end()
                routingContext.redirect("/");
            }
        });
        rc.next();
    }
}