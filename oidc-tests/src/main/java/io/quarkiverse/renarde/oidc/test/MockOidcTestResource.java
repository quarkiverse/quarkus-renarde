package io.quarkiverse.renarde.oidc.test;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;

public abstract class MockOidcTestResource<ConfigAnnotation extends Annotation>
        implements QuarkusTestResourceConfigurableLifecycleManager<ConfigAnnotation> {
    private HttpServer httpServer;
    protected String baseURI;
    private String name;
    private int port;

    MockOidcTestResource(String name, int port) {
        this.name = name;
        this.port = port;
    }

    @Override
    public Map<String, String> start() {
        System.err.println("Starting OIDC Mock: " + name);
        Vertx vertx = Vertx.vertx();
        HttpServerOptions options = new HttpServerOptions();
        options.setPort(port);
        httpServer = vertx.createHttpServer(options);

        Router router = Router.router(vertx);
        httpServer.requestHandler(router);
        registerRoutes(router);

        System.err.println("Going to listen");
        httpServer.listenAndAwait();
        int port = httpServer.actualPort();
        System.err.println("Listening on port " + port);

        Map<String, String> ret = new HashMap<>();
        baseURI = "http://localhost:" + port;
        ret.put("quarkus.oidc." + name + ".auth-server-url", baseURI);
        return ret;
    }

    protected abstract void registerRoutes(Router router);

    @Override
    public void stop() {
        System.err.println("Closing OIDC Mock: " + name);
        httpServer.closeAndAwait();
    }

}
