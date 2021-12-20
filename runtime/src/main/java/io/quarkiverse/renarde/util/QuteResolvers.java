package io.quarkiverse.renarde.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.enterprise.event.Observes;

import io.quarkiverse.renarde.router.Router;
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

    void configureEngine(@Observes EngineBuilder builder) {
        builder.addValueResolver(ValueResolver.builder()
                .appliesTo(ctx -> ctx.getBase() instanceof BoundRouter)
                .resolveSync(ctx -> {
                    List<Expression> params = ctx.getParams();
                    if (params.isEmpty()) {
                        return CompletedStage.of(findURI(ctx, Collections.emptyList()));
                    } else {
                        List<Uni<Object>> unis = new ArrayList<>(params.size());
                        for (int i = 0; i < params.size(); i++) {
                            CompletionStage<Object> val = ctx.evaluate(params.get(i));
                            Uni<Object> uni = Uni.createFrom().completionStage(val);
                            unis.add(uni);
                        }
                        return Uni.combine().all().unis(unis)
                                .collectFailures().combinedWith(paramValues -> findURI(ctx, paramValues))
                                .convert().toCompletionStage();
                    }
                })
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
}
