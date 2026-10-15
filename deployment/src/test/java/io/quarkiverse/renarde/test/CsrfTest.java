package io.quarkiverse.renarde.test;

import java.net.URL;

import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class CsrfTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class)
                    .addAsResource(new StringAsset("{#authenticityToken/}"), "templates/MyController/csrf.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testCsrfEnabled() {
        RestAssured
                .when()
                .get("/csrf").then()
                .statusCode(200)
                .body(Matchers.startsWith("<input type=\"hidden\" name=\"csrf-token\" value=\""));
    }

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance csrf();
        }

        @Path("/csrf")
        public TemplateInstance qute() {
            return Templates.csrf();
        }
    }
}
