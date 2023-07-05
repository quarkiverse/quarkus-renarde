package io.quarkiverse.renarde.test;

import java.net.URL;

import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class HxControllerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyHxController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testOOBConcat() {
        RestAssured
                .given()
                .when()
                .get("/oob").then()
                .statusCode(200)
                .body(Matchers.is("ab"));
    }

    @Test
    public void testHxHeader() {
        RestAssured
                .given()
                .when()
                .get("/hxHeader").then()
                .statusCode(200)
                .body(Matchers.is("header"))
                .header(HxController.HxResponseHeader.PUSH_URL.key(), "push");
    }

    @Test
    public void testOnlyHxFail() {
        RestAssured
                .given()
                .when()
                .get("/onlyHx").then()
                .statusCode(400);
    }

    @Test
    public void testOnlyHx() {
        RestAssured
                .given()
                .when()
                .header(HxController.HX_REQUEST_HEADER, "true")
                .get("/onlyHx").then()
                .statusCode(200)
                .body(Matchers.is("hx"));
    }

    @Test
    public void testRedirectHx() {
        RestAssured
                .given()
                .when()
                .header(HxController.HX_REQUEST_HEADER, "true")
                .get("/redirect").then()
                .statusCode(200)
                .body(Matchers.is("hx"));
    }

    @Test
    public void testRedirectHxFail() {
        RestAssured
                .given()
                .when()
                .get("/redirect").then()
                .statusCode(400);
    }

    @Path("/")
    public static class MyHxController extends HxController {

        public TemplateInstance oob() {
            return HxController.concatTemplates(Qute.fmt("a")
                    .cache().instance(),
                    Qute.fmt("b")
                            .cache().instance());
        }

        public void redirect() {
            this.onlyHx();
        }

        public String hxHeader() {
            hx(HxResponseHeader.PUSH_URL, "push");
            return "header";
        }

        public String onlyHx() {
            this.onlyHxRequest();
            return "hx";
        }
    }
}
