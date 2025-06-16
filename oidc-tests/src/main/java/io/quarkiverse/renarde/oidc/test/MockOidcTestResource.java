package io.quarkiverse.renarde.oidc.test;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.context.spi.ContextManagerProvider;

import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public abstract class MockOidcTestResource<ConfigAnnotation extends Annotation>
        implements QuarkusTestResourceConfigurableLifecycleManager<ConfigAnnotation> {
    private HttpServer httpServer;
    protected String baseURI;
    private String name;

    MockOidcTestResource(String name) {
        this.name = name;
    }

    @Override
    public Map<String, String> start() {
        Vertx vertx = Vertx.vertx();
        HttpServerOptions options = new HttpServerOptions();
        options.setPort(0);
        httpServer = vertx.createHttpServer(options);

        Router router = Router.router(vertx);
        httpServer.requestHandler(router);
        registerRoutes(router);

        try {
            httpServer.listen().toCompletionStage().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        int port = httpServer.actualPort();

        Map<String, String> ret = new HashMap<>();
        baseURI = "http://localhost:" + port;
        ret.put("quarkus.oidc." + name + ".auth-server-url", baseURI);

        System.err.println("Started OIDC Mock for " + name + " on " + baseURI);
        return ret;
    }

    protected abstract void registerRoutes(Router router);

    @Override
    public void stop() {
        System.err.println("Closing OIDC Mock: " + name);
        // we don't need to wait
        httpServer.close();
    }

    protected String hashAccessToken(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(string.getBytes(StandardCharsets.UTF_8));
            // keep 128 first bits, so 8 bytes
            byte[] part = new byte[8];
            System.arraycopy(digest, 0, part, 0, 8);
            return Base64.getUrlEncoder().encodeToString(part);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
