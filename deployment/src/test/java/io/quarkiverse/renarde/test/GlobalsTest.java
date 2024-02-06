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
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class GlobalsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class)
                    .addAsResource(new StringAsset("{request.url}\n"
                            + "{request.method}\n"
                            + "{request.scheme}\n"
                            + "{request.authority}\n"
                            + "{request.host}\n"
                            + "{request.port}\n"
                            + "{request.path}\n"
                            + "{request.action}\n"
                            + "{request.ssl}\n"
                            + "{request.remoteAddress}\n"
                    // This is null on tests
                    //                            + "{request.remoteHost}\n"
                    // This is hard to test
                    //                            + "{request.remotePort}\n"
                    ), "templates/MyController/globals.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testGlobals() {
        RestAssured
                .when()
                .get("/globals").then()
                .statusCode(200)
                .body(Matchers.is("http://localhost:8081/globals\n"
                        + "GET\n"
                        + "http\n"
                        + "localhost:8081\n"
                        + "localhost\n"
                        + "8081\n"
                        + "/globals\n"
                        + "MyController.globals\n"
                        + "false\n"
                        + "127.0.0.1\n"));
    }

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance globals();
        }

        @Path("/globals")
        public TemplateInstance globals() {
            return Templates.globals();
        }
    }
}
