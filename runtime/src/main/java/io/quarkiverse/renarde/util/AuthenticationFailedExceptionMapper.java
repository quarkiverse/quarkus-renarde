package io.quarkiverse.renarde.util;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.security.AuthenticationFailedException;
import io.vertx.core.http.HttpServerRequest;

@ApplicationScoped
public class AuthenticationFailedExceptionMapper {

    @Inject
    Flash flash;

    @Inject
    HttpServerRequest request;

    @ServerExceptionMapper(priority = Priorities.USER)
    public Response authenticationFailed(AuthenticationFailedException ex) {
        Throwable throwable = ex;
        while (throwable.getCause() != null)
            throwable = throwable.getCause();
        if (throwable instanceof InvalidJwtException) {
            InvalidJwtException x = (InvalidJwtException) throwable;
            if (x.hasErrorCode(ErrorCodes.EXPIRED)) {
                return redirectToRoot("Login expired, you've been logged out");
            }
            return redirectToRoot("Invalid session (bad JWT), you've been logged out");
        }
        // This happens when the private/public keys change, like in DEV mode
        if (throwable instanceof AEADBadTagException
                || throwable instanceof BadPaddingException) {
            return redirectToRoot("Invalid session (bad signature), you've been logged out");
        }
        // handle upstream
        return null;
    }

    private Response redirectToRoot(String message) {
        flash.flash("message", message);
        // FIXME: URI, perhaps redirect to login page?
        ResponseBuilder builder = Response.seeOther(URI.create("/"));
        // FIXME: constant
        NewCookie logoutCookie = new NewCookie.Builder("QuarkusUser").expiry(new Date(0)).build();
        builder.cookie(logoutCookie);
        Map<String, Object> map = new HashMap<>();
        // FIXME: format?
        map.put("message", message);
        Flash.setFlashCookie(request, request.response(), map);

        return builder.build();
    }
}
