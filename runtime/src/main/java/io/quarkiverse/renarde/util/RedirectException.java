package io.quarkiverse.renarde.util;

import io.quarkus.transaction.annotations.Rollback;
import jakarta.ws.rs.core.Response;

@Rollback(false)
@SuppressWarnings("serial")
public class RedirectException extends RuntimeException {

    public final Response response;

    public RedirectException(Response response) {
        this.response = response;
    }

}
