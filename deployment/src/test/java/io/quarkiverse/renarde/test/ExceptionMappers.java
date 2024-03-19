package io.quarkiverse.renarde.test;

import java.net.URL;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class ExceptionMappers {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class, MyException.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testExceptionMapper() {
        RestAssured
                .when()
                .get("/exception").then()
                .statusCode(200)
                .body(Matchers.is("OK"));
    }

    public static class MyException extends RuntimeException {
    }

    public static class MyController extends Controller {

        @Path("/exception")
        public String exception() {
            throw new MyException();
        }

        @ServerExceptionMapper
        public Response map(MyException x) {
            return Response.ok("OK").build();
        }
    }
}
