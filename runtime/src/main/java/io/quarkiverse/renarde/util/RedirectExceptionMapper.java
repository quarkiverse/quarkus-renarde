package io.quarkiverse.renarde.util;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class RedirectExceptionMapper {
    @ServerExceptionMapper
    public Response toResponse(RedirectException e) {
        return e.response;
    }
}
