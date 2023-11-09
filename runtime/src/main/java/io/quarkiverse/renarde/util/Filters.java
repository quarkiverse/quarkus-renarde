package io.quarkiverse.renarde.util;

import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class Filters {

    @Inject
    Flash flash;

    @Inject
    I18N i18n;

    @ServerRequestFilter
    public void filterRequest(ResteasyReactiveContainerRequestContext requestContext, HttpServerRequest req) {
        flash.handleFlashCookie();
        i18n.readLanguageCookie(requestContext);
    }

    // this must run before the Qute response filter
    @ServerResponseFilter(priority = Priorities.USER + 1000) // sorted in reverse order
    public void filterResponse(ContainerResponseContext responseContext, HttpServerResponse resp) {
        i18n.setLanguageCookie();
        flash.setFlashCookie();
    }
}
