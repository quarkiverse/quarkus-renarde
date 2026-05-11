package io.quarkiverse.renarde.test;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import jakarta.ws.rs.Path;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URL;

/**
 * Tests that overloaded methods work correctly across class inheritance —
 * superclass defines one overload, subclass defines another with different URI param count.
 */
public class InheritedOverloadTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BaseItemController.class, ExtendedItemController.class)
                    .addAsResource(new StringAsset(
                                    "{uri:ExtendedItemController.items()}\n"
                                    + "{uri:ExtendedItemController.items('electronics')}"),
                            "templates/ExtendedItemController/uris.txt") // copied
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testQuteResolvesInheritedOverloads() {
        // copied
        // The inherited items() generates a URI with the base class path (BaseItemController)
        // because the path was computed when scanning the declaring class. This is expected
        // inheritance behavior — the URI template comes from the class where the method is defined.
        String[] expectedLines = {
                "/BaseItemController/items",
                "/ExtendedItemController/items/electronics",
        };
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/uris")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals(String.join("\n", expectedLines), body);
    }

    @Test
    public void testEndpointsWork() {
        // copied
        String[][] endpoints = {
                {"/BaseItemController/items", "all"},
                {"/ExtendedItemController/items/electronics", "electronics"},
        };
        for (String[] endpoint : endpoints) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(endpoint[0])
                    .then().statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    public static abstract class BaseItemController extends Controller {
        // 0 URI params — list all items
        public String items() {
            return "all";
        }
    }

    public static class ExtendedItemController extends BaseItemController {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        // 1 URI param — filter by category (overloads inherited items())
        public String items(@RestPath String category) {
            return category;
        }
    }
}