package io.quarkiverse.renarde.util;

import jakarta.ws.rs.core.Response;

import io.quarkus.transaction.annotations.Rollback;

@Rollback(false)
@SuppressWarnings("serial")
public class RedirectException extends RuntimeException {

    public final Response response;

    public RedirectException(Response response) {
        this.response = response;
    }

}
