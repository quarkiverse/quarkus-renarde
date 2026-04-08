package io.quarkiverse.renarde.test;

import java.net.URL;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

/**
 * Tests multi-route resolution: overloaded methods, GET/POST same path,
 * closest-match fallback, inherited overloads, and non-URI param handling.
 */
public class RouteResolutionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ItemController.class, FormController.class,
                            FallbackController.class, BaseItemController.class,
                            ExtendedItemController.class, MixedParamController.class)
                    .addAsResource(new StringAsset(
                            """
                                    {uri:ItemController.items()}
                                    {uri:ItemController.items('electronics')}
                                    {uri:ItemController.items('electronics', '42')}"""),
                            "templates/ItemController/overloads.txt") //
                    .addAsResource(new StringAsset("{uri:FormController.submit()}"),
                            "templates/FormController/uris.txt")
                    .addAsResource(new StringAsset(
                            "{uri:FallbackController.items('electronics')}"),
                            "templates/FallbackController/uris.txt")
                    .addAsResource(new StringAsset(
                            """
                                    {uri:ExtendedItemController.items()}
                                    {uri:ExtendedItemController.items('electronics')}"""),
                            "templates/ExtendedItemController/uris.txt")
                    .addAsResource(new StringAsset(
                            """
                                    {uri:MixedParamController.withContext('hello', 'world')}
                                    {uri:MixedParamController.withFormParam('pathVal', 'queryVal')}
                                    {uri:MixedParamController.withHeaderParam('myPath', 'myQuery')}
                                    {uri:MixedParamController.withMultipleNonUri('id1', 'q1')}
                                    {uri:MixedParamController.withNonUriFirst('pathVal', 'queryVal')}
                                    {uri:MixedParamController.onlyPathAndQuery('p', 'q')}
                                    """), "templates/MixedParamController/uris.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    // --- Overloaded route tests (0, 1, 2 URI params) ---

    @Test
    public void testOverloadedQuteResolution() {
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

    @Test
    public void testOverloadedEndpoints() {
        String[][] endpoints = {
                { "/ItemController/items", "all" },
                { "/ItemController/items/electronics", "electronics" },
                { "/ItemController/items/electronics/42", "electronics|42" },
                { "/ItemController/items/books", "books" },
                { "/ItemController/items/books/7", "books|7" }
        };
        for (String[] endpoint : endpoints) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(endpoint[0])
                    .then().statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    @Test
    public void testOverloadedRouterGetUri() {
        String[][] cases = {
                { "/router-uri-test?overload=0", "/ItemController/items" },
                { "/router-uri-test?overload=1", "/ItemController/items/electronics" },
                { "/router-uri-test?overload=2", "/ItemController/items/electronics/42" },
        };
        for (String[] testCase : cases) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(testCase[0])
                    .then().statusCode(200)
                    .body(Matchers.is(testCase[1]));
        }
    }

    // --- GET/POST same name, same path ---

    @Test
    public void testGetPostSamePathQuteUri() {
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/form-uris")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertTrue(body.contains("/submit"),
                "Expected URI containing '/submit' but got: " + body);
    }

    @Test
    public void testGetPostSamePathGetEndpoint() {
        RestAssured.given().urlEncodingEnabled(false)
                .when().get("/submit")
                .then().statusCode(200)
                .body(Matchers.is("form-page"));
    }

    @Test
    public void testGetPostSamePathPostEndpoint() {
        RestAssured.given().urlEncodingEnabled(false)
                .formParam("data", "hello")
                .when().post("/submit")
                .then().statusCode(200)
                .body(Matchers.is("processed:hello"));
    }

    // --- Closest match fallback (wrong param count) ---

    @Test
    public void testClosestMatchFallback() {
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/fallback-uris")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals("/FallbackController/items", body);
    }

    // --- Inherited overloads (base + subclass) ---

    @Test
    public void testInheritedOverloadQuteResolution() {
        String[] expectedLines = {
                "/BaseItemController/items",
                "/ExtendedItemController/items/electronics",
        };
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/inherited-uris")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals(String.join("\n", expectedLines), body);
    }

    @Test
    public void testInheritedOverloadEndpoints() {
        String[][] endpoints = {
                { "/BaseItemController/items", "all" },
                { "/ExtendedItemController/items/electronics", "electronics" },
        };
        for (String[] endpoint : endpoints) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(endpoint[0])
                    .then().statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    // --- Non-URI params (@Context, @RestForm, @HeaderParam) ---

    @Test
    public void testNonUriParamQuteUri() {
        String body = RestAssured.when().get("/mixed-uris").then()
                .statusCode(200)
                .extract().body().asString();
        String[] expectedUris = {
                "/MixedParamController/withContext/hello?q=world",
                "/MixedParamController/withFormParam/pathVal?q=queryVal",
                "/MixedParamController/withHeaderParam/myPath?q=myQuery",
                "/MixedParamController/withMultipleNonUri/id1?q=q1",
                "/MixedParamController/withNonUriFirst/pathVal?q=queryVal",
                "/MixedParamController/onlyPathAndQuery/p?q=q",
        };
        String[] lines = body.split("\n");
        for (int i = 0; i < expectedUris.length; i++) {
            Assertions.assertEquals(expectedUris[i], lines[i],
                    "URI mismatch at line " + i);
        }
    }

    @Test
    public void testNonUriParamRouterGetUri() {
        String[][] endpoints = {
                { "/router-uri-context", "/MixedParamController/withContext/fromRouter?q=queryFromRouter" },
                { "/router-uri-form", "/MixedParamController/withFormParam/fromRouter?q=queryFromRouter" },
                { "/router-uri-header", "/MixedParamController/withHeaderParam/fromRouter?q=queryFromRouter" },
                { "/router-uri-multi", "/MixedParamController/withMultipleNonUri/fromRouter?q=queryFromRouter" },
        };
        for (String[] endpoint : endpoints) {
            RestAssured.when().get(endpoint[0]).then()
                    .statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    @Test
    public void testNonUriParamEndpoints() {
        String[][] endpoints = {
                { "/MixedParamController/withContext/hello?q=world", "hello|world" },
                { "/MixedParamController/withFormParam/pathVal?q=queryVal", "pathVal|queryVal" },
                { "/MixedParamController/withHeaderParam/myPath?q=myQuery", "myPath|myQuery" },
                { "/MixedParamController/withMultipleNonUri/id1?q=q1", "id1|q1" },
                { "/MixedParamController/withNonUriFirst/pathVal?q=queryVal", "pathVal|queryVal" },
                { "/MixedParamController/onlyPathAndQuery/p?q=q", "p|q" },
        };
        for (String[] endpoint : endpoints) {
            RestAssured.when().get(endpoint[0]).then()
                    .statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    // ========== Controller classes ==========

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

    public static class FormController extends Controller {
        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/form-uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        @Path("/submit")
        public String submit() {
            return "form-page";
        }

        @POST
        @Path("/submit")
        public String submit(@RestForm String data) {
            return "processed:" + data;
        }
    }

    public static class FallbackController extends Controller {
        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/fallback-uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        public String items() {
            return "all";
        }

        public String items(@RestPath String category, @RestPath String id) {
            return category + "|" + id;
        }
    }

    public static abstract class BaseItemController extends Controller {
        public String items() {
            return "all";
        }
    }

    public static class ExtendedItemController extends BaseItemController {
        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/inherited-uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        public String items(@RestPath String category) {
            return category;
        }
    }

    public static class MixedParamController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/mixed-uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        public String withContext(@RestPath String id, @Context UriInfo uriInfo, @RestQuery String q) {
            return id + "|" + q;
        }

        public String withFormParam(@RestPath String id, @RestForm String formField, @RestQuery String q) {
            return id + "|" + q;
        }

        public String withHeaderParam(@RestPath String id, @HeaderParam("X-Custom") String header, @RestQuery String q) {
            return id + "|" + q;
        }

        public String withMultipleNonUri(@RestPath String id,
                @Context UriInfo uriInfo,
                @RestForm String form1,
                @HeaderParam("X-Custom") String header,
                @RestQuery String q) {
            return id + "|" + q;
        }

        public String withNonUriFirst(@Context UriInfo uriInfo, @RestPath String id, @RestQuery String q) {
            return id + "|" + q;
        }

        public String onlyPathAndQuery(@RestPath String id, @RestQuery String q) {
            return id + "|" + q;
        }

        @Path("/router-uri-context")
        public String routerUriContext() {
            return Router.getURI(MixedParamController::withContext, "fromRouter", "queryFromRouter").toString();
        }

        @Path("/router-uri-form")
        public String routerUriForm() {
            return Router.getURI(MixedParamController::withFormParam, "fromRouter", "queryFromRouter").toString();
        }

        @Path("/router-uri-header")
        public String routerUriHeader() {
            return Router.getURI(MixedParamController::withHeaderParam, "fromRouter", "queryFromRouter").toString();
        }

        @Path("/router-uri-multi")
        public String routerUriMulti() {
            return Router.getURI(MixedParamController::withMultipleNonUri, "fromRouter", "queryFromRouter").toString();
        }
    }
}
