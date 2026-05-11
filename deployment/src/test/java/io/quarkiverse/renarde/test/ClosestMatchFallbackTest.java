package io.quarkiverse.renarde.test;

import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
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
 * Tests the runtime warn+closest-match fallback when a Qute template passes a param count
 * that doesn't exactly match any overload. The controller has 0-param and 2-param overloads
 * of {@code items()}, but the Qute template passes 1 param. The router should warn and use
 * the closest match (0 params, since |0-1| < |2-1|).
 */
public class ClosestMatchFallbackTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FallbackController.class)
                    .addAsResource(new StringAsset(
                            "{uri:FallbackController.items('electronics')}"),
                            "templates/FallbackController/uris.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testClosestMatchFallback() {
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/uris")
                .then().statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals("/FallbackController/items", body);
    }

    public static class FallbackController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("/uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }

        // 0 URI params
        public String items() {
            return "all";
        }

        // 2 URI params (path + path)
        public String items(@RestPath String category, @RestPath String id) {
            return category + "|" + id;
        }
    }
}