package io.quarkiverse.renarde.util;

import java.util.Map.Entry;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.quarkus.qute.TemplateInstance;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class Filters {

    @Inject
    RenderArgs renderArgs;

    @Inject
    Flash flash;

    @Inject
    CRSF crsf;

    @ServerRequestFilter
    public void filterRequest(HttpServerRequest req) {
        flash.handleFlashCookie();
        crsf.readCRSFCookie();
        // check CRSF param for every method except the three safe ones
        if (req.method() != HttpMethod.GET
                && req.method() != HttpMethod.HEAD
                && req.method() != HttpMethod.OPTIONS) {
            //FIXME: can't do this for now because form values are not read when filter is invoked
            //            crsf.checkCRSFToken();
        }
    }

    @ServerResponseFilter
    public void filterResponse(ContainerResponseContext responseContext, HttpServerResponse resp) {
        Object entity = responseContext.getEntity();
        if (entity instanceof TemplateInstance) {
            TemplateInstance template = (TemplateInstance) entity;
            for (Entry<String, Object> entry : renderArgs.entrySet()) {
                template.data(entry.getKey(), entry.getValue());
            }
        }
        flash.setFlashCookie();
        crsf.setCRSFCookie();
    }

}
