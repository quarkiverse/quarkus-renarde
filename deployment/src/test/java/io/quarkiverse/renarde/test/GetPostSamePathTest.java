package io.quarkiverse.renarde.test;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests that a controller can have GET and POST methods with the same name and same path,
 * and that URI generation works correctly (both produce the same URI since HTTP method
 * doesn't affect URI).
 */
public class GetPostSamePathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FormController.class)
                    .addAsResource(new StringAsset("{uri:FormController.submit()}"),
                            "templates/FormController/uris.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testQuteResolvesUri() {
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/uris")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertTrue(body.contains("/submit"),
                "Expected URI containing '/submit' but got: " + body);
    }

    @Test
    public void testGetEndpoint() {
        RestAssured.given().urlEncodingEnabled(false)
                .when().get("/submit")
                .then().statusCode(200)
                .body(Matchers.is("form-page"));
    }

    @Test
    public void testPostEndpoint() {
        RestAssured.given().urlEncodingEnabled(false)
                .formParam("data", "hello")
                .when().post("/submit")
                .then().statusCode(200)
                .body(Matchers.is("processed:hello"));
    }

    public static class FormController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        // GET: show the form page
        @Path("/submit")
        public String submit() {
            return "form-page";
        }

        // POST: process the form (same name, same path, different HTTP method)
        @POST
        @Path("/submit")
        public String submit(@RestForm String data) {
            return "processed:" + data;
        }
    }
}