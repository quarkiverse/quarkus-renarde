package io.quarkiverse.renarde.util;

import java.util.Map.Entry;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.MessageBundles;
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

    @Inject
    I18N i18n;

    @WithFormRead
    @ServerRequestFilter
    public void filterRequest(ResteasyReactiveContainerRequestContext requestContext, HttpServerRequest req) {
        flash.handleFlashCookie();
        crsf.readCRSFCookie();
        i18n.readLanguageCookie(requestContext);
        // check CRSF param for every method except the three safe ones
        if (req.method() != HttpMethod.GET
                && req.method() != HttpMethod.HEAD
                && req.method() != HttpMethod.OPTIONS) {
            //FIXME: can't do this for now because form values are not read when filter is invoked
            //            crsf.checkCRSFToken();
        }
    }

    // this must run before the Qute response filter
    @ServerResponseFilter(priority = 0)
    public void filterResponse(ContainerResponseContext responseContext, HttpServerResponse resp) {
        Object entity = responseContext.getEntity();
        if (entity instanceof TemplateInstance) {
            TemplateInstance template = (TemplateInstance) entity;
            for (Entry<String, Object> entry : renderArgs.entrySet()) {
                template.data(entry.getKey(), entry.getValue());
            }
            // set the proper locale
            // FIXME: this doesn't work ATM
            template.setAttribute(MessageBundles.ATTRIBUTE_LOCALE, i18n.get());
        }
        i18n.setLanguageCookie();
        flash.setFlashCookie();
        crsf.setCRSFCookie();
    }

}
