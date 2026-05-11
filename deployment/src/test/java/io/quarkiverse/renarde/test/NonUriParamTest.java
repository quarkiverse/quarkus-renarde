package io.quarkiverse.renarde.test;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import jakarta.ws.rs.HeaderParam;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URL;

/**
 * Tests that non-URI parameters (@Context, @RestForm, @HeaderParam, etc.)
 * are correctly skipped during URI varargs generation and don't consume
 * varargs slots that belong to path/query params.
 */
// TODO: copied
public class NonUriParamTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MixedParamController.class)
                    .addAsResource(new StringAsset(String.join("\n",
                                    "{uri:MixedParamController.withContext('hello', 'world')}",
                                    "{uri:MixedParamController.withFormParam('pathVal', 'queryVal')}",
                                    "{uri:MixedParamController.withHeaderParam('myPath', 'myQuery')}",
                                    "{uri:MixedParamController.withMultipleNonUri('id1', 'q1')}",
                                    "{uri:MixedParamController.withNonUriFirst('pathVal', 'queryVal')}",
                                    "{uri:MixedParamController.onlyPathAndQuery('p', 'q')}")),
                            "templates/MixedParamController/uris.txt") // copied
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testQuteUriWithNonUriParams() {
        // copied
        String body = RestAssured.when().get("/uris").then()
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
    public void testRouterGetUriWithNonUriParams() {
        // copied
        // Router.getURI should also work — varargs only contain path+query values
        String[][] endpoints = {
                {"/router-uri-context", "/MixedParamController/withContext/fromRouter?q=queryFromRouter"},
                {"/router-uri-form", "/MixedParamController/withFormParam/fromRouter?q=queryFromRouter"},
                {"/router-uri-header", "/MixedParamController/withHeaderParam/fromRouter?q=queryFromRouter"},
                {"/router-uri-multi", "/MixedParamController/withMultipleNonUri/fromRouter?q=queryFromRouter"},
        };
        for (String[] endpoint : endpoints) {
            RestAssured.when().get(endpoint[0]).then()
                    .statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    @Test
    public void testEndpointsWithNonUriParams() {
        // Verify endpoints themselves work (non-URI params get injected by the framework)
        // copied
        String[][] endpoints = {
                {"/MixedParamController/withContext/hello?q=world", "hello|world"},
                {"/MixedParamController/withFormParam/pathVal?q=queryVal", "pathVal|queryVal"},
                {"/MixedParamController/withHeaderParam/myPath?q=myQuery", "myPath|myQuery"},
                {"/MixedParamController/withMultipleNonUri/id1?q=q1", "id1|q1"},
                {"/MixedParamController/withNonUriFirst/pathVal?q=queryVal", "pathVal|queryVal"},
                {"/MixedParamController/onlyPathAndQuery/p?q=q", "p|q"},
        };
        for (String[] endpoint : endpoints) {
            RestAssured.when().get(endpoint[0]).then()
                    .statusCode(200)
                    .body(Matchers.is(endpoint[1]));
        }
    }

    // copied
    public static class MixedParamController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        // @Context param between path and query — should be skipped in varargs
        public String withContext(@RestPath String id, @Context UriInfo uriInfo, @RestQuery String q) {
            return id + "|" + q;
        }

        // @RestForm param between path and query — should be skipped in varargs
        public String withFormParam(@RestPath String id, @RestForm String formField, @RestQuery String q) {
            return id + "|" + q;
        }

        // @HeaderParam between path and query — should be skipped in varargs
        public String withHeaderParam(@RestPath String id, @HeaderParam("X-Custom") String header, @RestQuery String q) {
            return id + "|" + q;
        }

        // Multiple non-URI params interspersed — verifies correct index tracking
        public String withMultipleNonUri(@RestPath String id,
                                         @Context UriInfo uriInfo,
                                         @RestForm String form1,
                                         @HeaderParam("X-Custom") String header,
                                         @RestQuery String q) {
            return id + "|" + q;
        }

        // Non-URI param before any URI params
        public String withNonUriFirst(@Context UriInfo uriInfo, @RestPath String id, @RestQuery String q) {
            return id + "|" + q;
        }

        // Control case: only path and query params (no non-URI params)
        public String onlyPathAndQuery(@RestPath String id, @RestQuery String q) {
            return id + "|" + q;
        }

        // Helper endpoints to test Router.getURI with non-URI param methods
        @jakarta.ws.rs.Path("/router-uri-context")
        public String routerUriContext() {
            return Router.getURI(MixedParamController::withContext, "fromRouter", "queryFromRouter").toString();
        }

        @jakarta.ws.rs.Path("/router-uri-form")
        public String routerUriForm() {
            return Router.getURI(MixedParamController::withFormParam, "fromRouter", "queryFromRouter").toString();
        }

        @jakarta.ws.rs.Path("/router-uri-header")
        public String routerUriHeader() {
            return Router.getURI(MixedParamController::withHeaderParam, "fromRouter", "queryFromRouter").toString();
        }

        @jakarta.ws.rs.Path("/router-uri-multi")
        public String routerUriMulti() {
            return Router.getURI(MixedParamController::withMultipleNonUri, "fromRouter", "queryFromRouter").toString();
        }
    }
}