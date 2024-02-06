package io.quarkiverse.renarde.util;

import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkus.arc.Arc;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.qute.TemplateGlobal;
import io.vertx.ext.web.RoutingContext;

@TemplateGlobal
public class Globals {
    /**
     * Variant of inject:csrf that can actually be null if CSRF is disabled via configuration. Otherwise Qute
     * will validate that it's here and throw at compile time if CSRF is disabled.
     * FIXME: use https://github.com/quarkusio/quarkus/pull/36524 once released
     */
    public static CsrfTokenParameterProvider csrf() {
        return Arc.container().instance(CsrfTokenParameterProvider.class).get();
    }

    public static class RenardeRequest {
        public static final RenardeRequest INSTANCE = new RenardeRequest();

        public String getUrl() {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            return request.getAbsoluteURI();
        }

        public String getPath() {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            return request.getPath();
        }

        public String getScheme() {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            return request.getScheme();
        }

        public String getAuthority() {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            return request.getAuthority();
        }

        public String getMethod() {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            return request.getMethod();
        }

        public String getHost() {
            RoutingContext ctx = Arc.container().instance(RoutingContext.class).get();
            return ctx.request().authority().host();
        }

        public int getPort() {
            RoutingContext ctx = Arc.container().instance(RoutingContext.class).get();
            return ctx.request().authority().port();
        }

        public boolean isSsl() {
            RoutingContext ctx = Arc.container().instance(RoutingContext.class).get();
            return ctx.request().isSSL();
        }

        public String getRemoteAddress() {
            RoutingContext ctx = Arc.container().instance(RoutingContext.class).get();
            return ctx.request().remoteAddress().hostAddress();
        }

        public String getRemoteHost() {
            RoutingContext ctx = Arc.container().instance(RoutingContext.class).get();
            return ctx.request().remoteAddress().hostName();
        }

        public int getRemotePort() {
            RoutingContext ctx = Arc.container().instance(RoutingContext.class).get();
            return ctx.request().remoteAddress().port();
        }

        public String getAction() {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            SimpleResourceInfo info = request.getTarget().getSimplifiedResourceInfo();
            return info.getResourceClass().getSimpleName() + "." + info.getMethodName();
        }
    }

    public static RenardeRequest request() {
        return RenardeRequest.INSTANCE;
    }
}
