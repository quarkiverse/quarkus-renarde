package io.quarkiverse.renarde.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.crypto.AEADBadTagException;

import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

//@ApplicationScoped
public class AuthenticationFailedExceptionMapper {

    //     This doesn't work:
    //    @Inject
    //    Flash flash;
    //
    //    @ServerExceptionMapper(priority = Priorities.USER)
    //    public Response authenticationFailed(AuthenticationFailedException x) {
    //        Throwable throwable = x;
    //        System.err.println("Original exception: " + throwable);
    //        while (throwable.getCause() != null)
    //            throwable = throwable.getCause();
    //        System.err.println("Exception cause: " + throwable);
    //        if (throwable instanceof InvalidJwtException) {
    //            InvalidJwtException jwtEx = (InvalidJwtException) throwable;
    //            if (jwtEx.hasErrorCode(ErrorCodes.EXPIRED)) {
    //                return redirectToRoot(true, "Login expired, you've been logged out");
    //            }
    //        }
    //        // This happens when the private/public keys change, like in DEV mode
    //        if (throwable instanceof AEADBadTagException) {
    //            return redirectToRoot(true, "Something is rotten about your JWT, clearing it");
    //        }
    //        // log the exception?
    //        x.printStackTrace();
    //        return redirectToRoot(false, "Authentication failed");
    //    }
    //
    //    private Response redirectToRoot(boolean logout, String message) {
    //        flash.flash("message", message);
    //        // FIXME: URI, perhaps redirect to login page?
    //        ResponseBuilder builder = Response.seeOther(URI.create("/"));
    //        if (logout) {
    //            // FIXME: constant
    //            NewCookie logoutCookie = new NewCookie("QuarkusUser", "", "/", null, NewCookie.DEFAULT_VERSION, null,
    //                    NewCookie.DEFAULT_MAX_AGE, null, false, false);
    //            builder.cookie(logoutCookie);
    //        }
    //        return builder.build();
    //    }

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
                    redirectToRoot(routingContext, "Invalid session (bad JWT), you've been logged out");
                }
                // This happens when the private/public keys change, like in DEV mode
                if (throwable instanceof AEADBadTagException) {
                    redirectToRoot(routingContext, "Invalid session (bad signature), you've been logged out");
                    return;
                }
                // let upstream handle the rest
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
                routingContext.response().setStatusCode(303);
                routingContext.redirect("/");
            }
        });
        rc.next();
    }
}
