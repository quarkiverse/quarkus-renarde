package io.quarkiverse.renarde.util;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.Response;

public class RedirectExceptionMapper {
    @ServerExceptionMapper
    public Response toResponse(RedirectException e) {
        return e.response;
    }
}
