package io.quarkiverse.renarde.test;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Method0;
import io.quarkiverse.renarde.router.Method1;
import io.quarkiverse.renarde.router.Method2;
import io.quarkiverse.renarde.router.Router;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import jakarta.ws.rs.Path;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URL;

/**
 * Tests that overloaded controller methods with different URI param counts are
 * correctly registered and resolved via Qute templates and Router.getURI.
 */
// TODO: copied
public class OverloadedRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ItemController.class)
                    .addAsResource(new StringAsset(
                                    "{uri:ItemController.items()}\n"
                                    + "{uri:ItemController.items('electronics')}\n"
                                    + "{uri:ItemController.items('electronics', '42')}"),
                            "templates/ItemController/overloads.txt") // TODO: exact copy
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testQuteResolvesCorrectOverload() {
        // TODO: exact
        String[] expectedLines = {
                "/ItemController/items",
                "/ItemController/items/electronics",
                "/ItemController/items/electronics/42",
        };
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/overloads")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals(String.join("\n", expectedLines), body);
    }

    @Test // TODO: copied
    public void testEndpointsWork() {
        String[][] endpoints = {
                {"/ItemController/items", "all"},
                {"/ItemController/items/electronics", "electronics"},
                {"/ItemController/items/electronics/42", "electronics|42"},
                {"/ItemController/items/books", "books"},
                {"/ItemController/items/books/7", "books|7"},
        };
        for (String[] endpoint : endpoints) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(endpoint[0])
                    .then().statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    @Test// TODO: exact copy (just diff name)
    public void testRouterGetUri() {
        String[][] cases = {
                {"/router-uri-test?overload=0", "/ItemController/items"},
                {"/router-uri-test?overload=1", "/ItemController/items/electronics"},
                {"/router-uri-test?overload=2", "/ItemController/items/electronics/42"},
        };
        for (String[] testCase : cases) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(testCase[0])
                    .then().statusCode(200)
                    .body(Matchers.is(testCase[1]));
        }
    }

    // TODO: exact copy
    public static class ItemController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance overloads();
        }

        @Path("/overloads")
        public TemplateInstance overloads() {
            return Templates.overloads();
        }

        // Overload 0: 0 URI params → /ItemController/items
        public String items() {
            return "all";
        }

        // Overload 1: 1 URI param → /ItemController/items/{category}
        public String items(@RestPath String category) {
            return category;
        }

        // Overload 2: 2 URI params → /ItemController/items/{category}/{id}
        public String items(@RestPath String category, @RestPath String id) {
            return category + "|" + id;
        }

        @Path("/router-uri-test")
        public String routerUriTest(@RestQuery int overload) {
            return switch (overload) {
                case 0 -> Router.getURI((Method0<ItemController>) ItemController::items).toString();
                case 1 -> Router.getURI((Method1<ItemController, String>) ItemController::items, "electronics")
                        .toString();
                case 2 -> Router.getURI((Method2<ItemController, String, String>) ItemController::items, "electronics",
                        "42").toString();
                default -> "unknown";
            };
        }
    }
}