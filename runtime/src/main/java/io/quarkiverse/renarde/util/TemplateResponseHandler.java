package io.quarkiverse.renarde.util;

import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;

public class TemplateResponseHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        Object result = requestContext.getResult();
        // we run in the first phase, so we could have a TemplateInstance, a Uni<TemplateInstance> or a CompletionStage<TemplateInstance>
        // we do not care about Reponse and RestResponse, this is handled by Filters
        if (result instanceof TemplateInstance) {
            setRenderArgsAndLocale(requestContext, (TemplateInstance) result);
        } else if (result instanceof Uni) {
            Uni<?> res = (Uni<?>) result;
            // hook into the Uni
            requestContext.setResult(res.invoke(resolved -> {
                if (resolved instanceof TemplateInstance) {
                    setRenderArgsAndLocale(requestContext, (TemplateInstance) resolved);
                }
            }));
        } else if (result instanceof CompletionStage) {
            CompletionStage<?> res = (CompletionStage<?>) result;
            // hook into the CompletionStage
            requestContext.setResult(res.thenApply(resolved -> {
                if (resolved instanceof TemplateInstance) {
                    setRenderArgsAndLocale(requestContext, (TemplateInstance) resolved);
                }
                return resolved;
            }));
        }
    }

    private void setRenderArgsAndLocale(ResteasyReactiveRequestContext requestContext, TemplateInstance templateInstance) {
        requestContext.requireCDIRequestScope();
        Filters filters = Arc.container().instance(Filters.class).get();
        filters.setTemplateLocaleAndRenderArgs(templateInstance);
    }

}
