package io.quarkiverse.renarde.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import jakarta.enterprise.event.Observes;

import io.quarkiverse.renarde.router.Router;
import io.quarkus.arc.Arc;
import io.quarkus.qute.*;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class QuteResolvers {

    static class BoundRouter {
        public final String target;
        public final boolean absolute;

        public BoundRouter(String target, boolean absolute) {
            this.target = target;
            this.absolute = absolute;
        }
    }

    static class MessageKey {
        public final String key;
        public boolean isPendingAppendOperation;

        public MessageKey(String key) {
            if (key.startsWith("'") && key.endsWith("'")) {
                this.key = key.substring(1, key.length() - 1);
            } else {
                this.key = key;
            }
        }

        public MessageKey append(String postfix, boolean addDot) {
            if (!addDot) {
                return new MessageKey(key + postfix);
            } else {
                return new MessageKey(key + "." + postfix);
            }
        }

        // used by Qute for the rendering without parameters
        public String toString() {
            return render();
        }

        public String render(Object... params) {
            I18N i18n = Arc.container().instance(I18N.class).get();
            String message = i18n.getMessage(key);
            // try to be helpful if the key doesn't match
            if (message == null) {
                return key;
            }
            return String.format(message, params);
        }

        public Object renderIfParameters(EvalContext ctx) {
            if (ctx.getParams().isEmpty()) {
                return this;
            } else {
                return evaluateParameters(ctx, (ctx2, params) -> render(params.toArray()));
            }
        }
    }

    void configureEngine(@Observes EngineBuilder builder) {
        builder.addValueResolver(ValueResolver.builder()
                .appliesTo(ctx -> (ctx.getBase() instanceof MessageKey))
                .resolveSync(ctx -> {
                    MessageKey base = ((MessageKey) ctx.getBase());
                    String name = ctx.getName().strip();
                    if (name.equals("+")) {
                        if (ctx.getParams().isEmpty()) {
                            // if the + is not followed by parameter, or a direct new line, for example if it is followed by a space then a new line
                            // we will get empty parameters and get called again later with what's on the next line, so remember we wanted to append
                            base.isPendingAppendOperation = true;
                            return base;
                        }
                        base.isPendingAppendOperation = false;
                        return evaluateParameters(ctx, (ctx2, params) -> {
                            if (params.size() == 1) {
                                return base.append(params.get(0).toString(), false);
                            } else {
                                throw new RuntimeException("'+' operator must have exactly one right-hand-side expressions");
                            }
                        });
                    } else {
                        if (base.isPendingAppendOperation) {
                            // remove quoting
                            // this one is nuts, but I've seen it
                            if (name.endsWith(")")) {
                                name = name.substring(0, name.length() - 1);
                            }
                            if (name.startsWith("'") && name.endsWith("'")) {
                                name = name.substring(1, name.length() - 1);
                            }
                        }
                        MessageKey key = base.append(name, !base.isPendingAppendOperation);
                        base.isPendingAppendOperation = false;
                        return key.renderIfParameters(ctx);
                    }
                })
                .build());
        builder.addNamespaceResolver(NamespaceResolver.builder("m")
                .resolve(ctx -> new MessageKey(ctx.getName()).renderIfParameters(ctx))
                .build());
        builder.addValueResolver(ValueResolver.builder()
                .appliesTo(ctx -> ctx.getBase() instanceof BoundRouter)
                .resolveSync(ctx -> evaluateParameters(ctx, this::findURI))
                .build());
        builder.addNamespaceResolver(NamespaceResolver.builder("uri")
                .resolve(ctx -> new BoundRouter(ctx.getName(), false))
                .build());
        builder.addNamespaceResolver(NamespaceResolver.builder("uriabs")
                .resolve(ctx -> new BoundRouter(ctx.getName(), true))
                .build());
    }

    void registerTemplateInstanceLocaleAndRenderArgs(@Observes EngineBuilder engineBuilder, I18N i18n, RenderArgs renderArgs) {

        if (ProfileManager.getLaunchMode().isDevOrTest()) {
            engineBuilder.addTemplateInstanceInitializer(templateInstance -> {
                if (!Arc.container().requestContext().isActive()) {
                    return;
                }
                RoutingContext routingContext = Arc.container().instance(RoutingContext.class).get();
                MultiMap headers = routingContext.response().headers();
                Template template = templateInstance.getTemplate();
                boolean isFragment = template.isFragment();
                String parentTemplateId = "";
                if (isFragment) {
                    var fragment = (Template.Fragment) template;
                    parentTemplateId = fragment.getOriginalTemplate().getId() + ".";
                }

                // if we send an email before returning the template the headers get added twice
                headers.remove("X-Template").add("X-Template", parentTemplateId + template.getId());
                headers.remove("X-Fragment").add("X-Fragment", Boolean.toString(isFragment));
            });
        }

        engineBuilder.addTemplateInstanceInitializer(templateInstance -> {
            // This should work if `I18N` is a request scoped bean
            if (Arc.container().requestContext().isActive()) {
                if (i18n.getLocale() != null
                        && templateInstance.getAttribute(MessageBundles.ATTRIBUTE_LOCALE) == null) {
                    templateInstance.setAttribute(MessageBundles.ATTRIBUTE_LOCALE, i18n.getLocale());
                }
                // extra parameters
                for (Entry<String, Object> entry : renderArgs.entrySet()) {
                    templateInstance.data(entry.getKey(), entry.getValue());
                }
            }
        });
    }

    private URI findURI(EvalContext ctx, List<?> paramValues) {
        BoundRouter boundRouter = (BoundRouter) ctx.getBase();
        // FIXME: make it work for multiple sets of parameters? with optional query params that's a bit harder
        // but probably GET/PUT/POST/DELETE will all have the same required set and URI?
        String route = boundRouter.target + "." + ctx.getName();
        return Router.findURI(route, boundRouter.absolute, paramValues.toArray());
    }

    static <R> CompletionStage<R> evaluateParameters(EvalContext ctx, BiFunction<EvalContext, List<?>, R> mapper) {
        List<Expression> params = ctx.getParams();
        if (params.isEmpty()) {
            return CompletedStage.of(mapper.apply(ctx, Collections.emptyList()));
        } else {
            List<Uni<Object>> unis = new ArrayList<>(params.size());
            for (int i = 0; i < params.size(); i++) {
                CompletionStage<Object> val = ctx.evaluate(params.get(i));
                Uni<Object> uni = Uni.createFrom().completionStage(val);
                unis.add(uni);
            }
            return Uni.combine().all().unis(unis)
                    .collectFailures().combinedWith(paramValues -> mapper.apply(ctx, paramValues))
                    .convert().toCompletionStage();
        }
    }
}
