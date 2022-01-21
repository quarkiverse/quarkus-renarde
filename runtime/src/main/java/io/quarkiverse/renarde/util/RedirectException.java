package io.quarkiverse.renarde.util;

import javax.ws.rs.core.Response;

import io.quarkus.narayana.jta.Rollback;

@Rollback(false)
@SuppressWarnings("serial")
public class RedirectException extends RuntimeException {

    public final Response response;

    public RedirectException(Response response) {
        this.response = response;
    }

}
