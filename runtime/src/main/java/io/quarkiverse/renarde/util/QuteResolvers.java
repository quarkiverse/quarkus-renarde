package io.quarkiverse.renarde.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import jakarta.enterprise.event.Observes;

import io.quarkiverse.renarde.router.Router;
import io.quarkus.arc.Arc;
import io.quarkus.qute.CompletedStage;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.ValueResolver;
import io.smallrye.mutiny.Uni;

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
                    System.err.println("base: " + base + " name: " + name + " params: " + ctx.getParams());
                    if (name.equals("+")) {
                        return evaluateParameters(ctx, (ctx2, params) -> {
                            if (params.size() == 1) {
                                return base.append(params.get(0).toString(), false);
                            } else {
                                throw new RuntimeException("'+' operator must have exactly one right-hand-side expressions");
                            }
                        });
                    } else {
                        MessageKey key = ((MessageKey) ctx.getBase()).append(name, true);
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
