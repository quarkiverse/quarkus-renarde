package io.quarkiverse.renarde.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkiverse.renarde.impl.RenardeConfig;
import io.quarkus.security.AuthenticationFailedException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@ApplicationScoped
public class AuthenticationFailedExceptionMapper {

    @Inject
    Flash flash;

    @Inject
    HttpServerRequest request;

    @Inject
    RenardeConfig config;

    @ConfigProperty(name = "mp.jwt.token.cookie")
    String jwtCookie;

    @ServerExceptionMapper(priority = Priorities.USER)
    public Response authenticationFailed(AuthenticationFailedException ex, HttpServerResponse vertxResponse) {
        Throwable throwable = ex;
        while (throwable.getCause() != null)
            throwable = throwable.getCause();
        if (throwable instanceof InvalidJwtException) {
            InvalidJwtException x = (InvalidJwtException) throwable;
            if (x.hasErrorCode(ErrorCodes.EXPIRED)) {
                return redirectToRoot("Login expired, you've been logged out", vertxResponse);
            }
            return redirectToRoot("Invalid session (bad JWT), you've been logged out", vertxResponse);
        }
        // This happens when the private/public keys change, like in DEV mode
        if (throwable instanceof AEADBadTagException
                || throwable instanceof BadPaddingException) {
            return redirectToRoot("Invalid session (bad signature), you've been logged out", vertxResponse);
        }
        // handle upstream
        return null;
    }

    private Response redirectToRoot(String message, HttpServerResponse vertxResponse) {
        flash.flash("message", message);
        // see https://github.com/quarkiverse/quarkus-renarde/issues/194
        if (vertxResponse.headers().contains(HttpHeaders.LOCATION)) {
            // workaround bug where auth challenge sets location header, and we add one, resulting
            // in there being two, which is invalid HTTP
            vertxResponse.headers().remove(HttpHeaders.LOCATION);
        }
        ResponseBuilder builder = Response.seeOther(URI.create(config.getLoginPage()));
        builder.cookie(invalidateCookie(jwtCookie));
        Map<String, Object> map = new HashMap<>();
        // FIXME: format?
        map.put("message", message);
        Flash.setFlashCookie(request, request.response(), map);

        return builder.build();
    }

    public static NewCookie invalidateCookie(String cookieName) {
        return new NewCookie.Builder(cookieName).path("/").maxAge(0).build();
    }

}
