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
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class LanguageDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class, MyController.Templates.class)
                    // Renarde type-unsafe
                    .addAsResource(new StringAsset("my_greeting=english message"),
                            "messages.properties")
                    .addAsResource(new StringAsset("my_greeting=message français"),
                            "messages_fr.properties")
                    .addAsResource(new StringAsset("{m:my_greeting}"),
                            "templates/MyController/typeUnsafe.txt")

                    .addAsResource(new StringAsset("quarkus.locales=en,fr\n"
                            + "quarkus.default-locale=en"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testAcceptLanguage() {
        RestAssured
                .given()
                .header("Accept-Language", "fr")
                .when()
                .get("/type-unsafe").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
        RestAssured
                .given()
                .when()
                .get("/type-unsafe").then()
                .statusCode(200)
                .body(Matchers.is("english message"));
        RestAssured
                .given()
                .header("Accept-Language", "fr")
                .when()
                .get("/manual").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
        RestAssured
                .given()
                .when()
                .get("/manual").then()
                .statusCode(200)
                .body(Matchers.is("english message"));
        config.modifyResourceFile("messages.properties", txt -> txt.replace("english message", "ENGLISH"));
        config.modifyResourceFile("messages_fr.properties", txt -> txt.replace("message français", "FRANÇAIS"));
        RestAssured
                .given()
                .header("Accept-Language", "fr")
                .when()
                .get("/type-unsafe").then()
                .statusCode(200)
                .body(Matchers.is("FRANÇAIS"));
        RestAssured
                .given()
                .when()
                .get("/type-unsafe").then()
                .statusCode(200)
                .body(Matchers.is("ENGLISH"));
        RestAssured
                .given()
                .header("Accept-Language", "fr")
                .when()
                .get("/manual").then()
                .statusCode(200)
                .body(Matchers.is("FRANÇAIS"));
        RestAssured
                .given()
                .when()
                .get("/manual").then()
                .statusCode(200)
                .body(Matchers.is("ENGLISH"));
    }

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance typeUnsafe();
        }

        @Path("/type-unsafe")
        public TemplateInstance typeUnsafe() {
            return Templates.typeUnsafe();
        }

        @Path("/manual")
        public String manual() {
            return i18n.formatMessage("my_greeting");
        }
    }
}
