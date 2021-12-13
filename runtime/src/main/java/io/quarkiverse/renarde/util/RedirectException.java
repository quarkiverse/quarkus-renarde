package io.quarkiverse.renarde.util;

import javax.ws.rs.core.Response;

import io.quarkus.narayana.jta.DontRollback;

@DontRollback
@SuppressWarnings("serial")
public class RedirectException extends RuntimeException {

    public final Response response;

    public RedirectException(Response response) {
        this.response = response;
    }

}
