package io.quarkiverse.renarde.util;

import java.util.Map.Entry;

import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.MessageBundles;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class Filters {

    @Inject
    RenderArgs renderArgs;

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
        Object entity = responseContext.getEntity();
        // this pass only handles methods that return Response or RestResponse, the others are handled in TemplateResponseHandler
        if (entity instanceof TemplateInstance) {
            TemplateInstance template = (TemplateInstance) entity;
            setTemplateLocaleAndRenderArgs(template);
        }
        i18n.setLanguageCookie();
        flash.setFlashCookie();
    }

    void setTemplateLocaleAndRenderArgs(TemplateInstance template) {
        // extra parameters
        for (Entry<String, Object> entry : renderArgs.entrySet()) {
            template.data(entry.getKey(), entry.getValue());
        }
        // set the proper locale
        template.setAttribute(MessageBundles.ATTRIBUTE_LOCALE, i18n.getLanguage());
    }

}
