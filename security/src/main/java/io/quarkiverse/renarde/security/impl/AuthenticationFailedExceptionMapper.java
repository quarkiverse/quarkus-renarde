package io.quarkiverse.renarde.security.impl;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkus.security.AuthenticationFailedException;
import io.vertx.core.http.HttpServerResponse;

@ApplicationScoped
public class AuthenticationFailedExceptionMapper {

    @Inject
    RenardeSecurity renardeSecurity;

    @ServerExceptionMapper(priority = Priorities.USER)
    public Response authenticationFailed(AuthenticationFailedException ex,
            HttpServerResponse vertxResponse) {
        // This wants to fix issues with the JWT cookie, not with the OIDC one, so if we don't have a JWT user cookie, let upstream
        // handle it
        if (!renardeSecurity.hasUserCookie()) {
            return null;
        }
        Throwable throwable = ex;
        while (throwable.getCause() != null)
            throwable = throwable.getCause();
        if (throwable instanceof InvalidJwtException) {
            InvalidJwtException x = (InvalidJwtException) throwable;
            if (x.hasErrorCode(ErrorCodes.EXPIRED)) {
                return renardeSecurity.makeRedirectToLogin("Login expired, you've been logged out");
            }
            return renardeSecurity.makeRedirectToLogin("Invalid session (bad JWT), you've been logged out");
        }
        // This happens when the private/public keys change, like in DEV mode
        if (throwable instanceof AEADBadTagException
                || throwable instanceof BadPaddingException) {
            return renardeSecurity.makeRedirectToLogin("Invalid session (bad signature), you've been logged out");
        }
        // handle upstream
        return null;
    }

}
