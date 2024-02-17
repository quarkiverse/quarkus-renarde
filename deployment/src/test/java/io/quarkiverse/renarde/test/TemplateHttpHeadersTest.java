package io.quarkiverse.renarde.test;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.ws.rs.Path;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.when;

public class TemplateHttpHeadersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MyController.class)
                            .addAsResource(new StringAsset("""
                                    template content
                                    {#fragment id="fragment" rendered=false}
                                    fragment template
                                    {/fragment}
                                    """), "templates/MyController/template.html")
            );

    @Test
    public void testTemplateHeaders() {

        when()
                .get("/template").then()
                .header("X-Fragment", "false")
                .header("X-Template", "MyController/template.html")
                .statusCode(200)
                .body(Matchers.is("template content\n"));

        when()
                .get("/fragment").then()
                .header("X-Fragment", "true")
                .header("X-Template", "MyController/template.html.fragment")
                .statusCode(200)
                .body(Matchers.is("fragment template\n"));
    }

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance template();
            public static native TemplateInstance template$fragment();
        }

        @Path("/template")
        public TemplateInstance template() {
            return Templates.template();
        }

        @Path("/fragment")
        public TemplateInstance fragment() {
            return Templates.template$fragment();
        }

    }
}
