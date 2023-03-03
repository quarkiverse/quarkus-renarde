package io.quarkiverse.renarde.test;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.util.I18N;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;

public class LanguageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class, MyBundle.class)
                    // Type-safe
                    .addAsResource(new StringAsset("my_greeting=english message"), "messages/msg_en.properties")
                    .addAsResource(new StringAsset("my_greeting=message français"), "messages/msg_fr.properties")
                    .addAsResource(new StringAsset("{msg:my_greeting}"), "templates/MyController/qute.txt")
                    // Renarde type-unsafe
                    .addAsResource(new StringAsset("my_greeting=english message\n"
                            + "params=english %s message\n"
                            + "my.greeting=english message\n"
                            + "my.greeting.something=english message\n"
                            + "my.greeting.code.something=english message with code\n"
                            + "my.params=english %s message\n"
                            + "my.params.something=english %s message"),
                            "messages.properties")
                    .addAsResource(new StringAsset("my_greeting=message français\n" +
                            "params=message %s français\n"
                            + "my.greeting=message français\n"
                            + "my.greeting.something=message français\n"
                            + "my.greeting.code.something=message français avec code\n"
                            + "my.params=message %s français\n"
                            + "my.params.something=message %s français"),
                            "messages_fr.properties")
                    .addAsResource(new StringAsset("{m:my_greeting}\n"
                            + "{m:params('STEF')}\n"
                            + "{m:my.greeting}\n"
                            + "{m:'my.greeting.' + 'something'}\n"
                            + "{m:'my.greeting.' + val + \n '.something'}\n"
                            + "{m:'my.greeting.' + val +\n '.something'}\n"
                            + "{m:my.params('STEF')}\n"
                            + "{m:missing}"),
                            "templates/MyController/typeUnsafe.txt")

                    .addAsResource(new StringAsset("quarkus.locales=en,fr\n"
                            + "quarkus.default-locale=en"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testDefaultLanguage() {
        RestAssured
                .when()
                .get("/lang").then()
                .statusCode(200)
                .body(Matchers.is("en"));
    }

    @Test
    public void testChangeLanguage() {
        CookieFilter cookieFilter = new CookieFilter();
        RestAssured
                .given()
                .filter(cookieFilter)
                .when()
                .get("/lang").then()
                .statusCode(200)
                .body(Matchers.is("en"));
        RestAssured
                .given()
                .filter(cookieFilter)
                .when()
                .param("l", "fr")
                .post("/lang").then()
                .statusCode(200)
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .body(Matchers.is("fr"));
        RestAssured
                .given()
                .filter(cookieFilter)
                .when()
                .get("/lang").then()
                .statusCode(200)
                .body(Matchers.is("fr"));
    }

    @Test
    public void testAcceptLanguage() {
        RestAssured
                .given()
                .header("Accept-Language", "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5")
                .when()
                .get("/lang").then()
                .statusCode(200)
                .body(Matchers.is("fr"));
        RestAssured
                .given()
                .header("Accept-Language", "*")
                .when()
                .get("/lang").then()
                .statusCode(200)
                .body(Matchers.is("en"));
        RestAssured
                .given()
                .header("Accept-Language", "de")
                .when()
                .get("/lang").then()
                .statusCode(200)
                .body(Matchers.is("en"));
    }

    @Test
    public void testQuteLanguage() {
        RestAssured
                .get("/qute").then()
                .statusCode(200)
                .body(Matchers.is("english message"));
        RestAssured
                .given()
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .get("/qute").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
        RestAssured
                .given()
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .get("/qute-uni").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
        RestAssured
                .given()
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .get("/qute-cs").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
        RestAssured
                .given()
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .get("/qute-response").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
        RestAssured
                .given()
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .get("/qute-rest-response").then()
                .statusCode(200)
                .body(Matchers.is("message français"));
    }

    @Test
    public void testTypeUnsafeLanguage() {
        RestAssured
                .get("/type-unsafe").then()
                .statusCode(200)
                .body(Matchers.is(
                        "english message\nenglish STEF message\nenglish message\nenglish message\nenglish message with code\nenglish message with code\nenglish STEF message\nmissing"));
        RestAssured
                .given()
                .cookie(I18N.LANGUAGE_COOKIE_NAME, "fr")
                .get("/type-unsafe").then()
                .statusCode(200)
                .body(Matchers.is(
                        "message français\nmessage STEF français\nmessage français\nmessage français\nmessage français avec code\nmessage français avec code\nmessage STEF français\nmissing"));
        // now try a missing language
        RestAssured
                .given()
                .param("l", "de")
                .post("/lang").then()
                .statusCode(500);

    }

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance qute();

            public static native TemplateInstance typeUnsafe(Object val);
        }

        @Path("/qute")
        public TemplateInstance qute() {
            return Templates.qute();
        }

        @Path("/qute-cs")
        public CompletionStage<TemplateInstance> quteCs() {
            return CompletableFuture.completedFuture(Templates.qute());
        }

        @Path("/qute-uni")
        public Uni<TemplateInstance> quteUni() {
            return Uni.createFrom().item(Templates.qute());
        }

        @Path("/qute-response")
        public Response quteResponse() {
            return Response.ok(Templates.qute()).build();
        }

        @Path("/qute-rest-response")
        public RestResponse<TemplateInstance> quteRestResponse() {
            return RestResponse.ok(Templates.qute());
        }

        @Path("/type-unsafe")
        public TemplateInstance typeUnsafe() {
            return Templates.typeUnsafe("code");
        }

        @Path("/lang")
        public String lang() {
            return i18n.get();
        }

        @POST
        @Path("/lang")
        public String lang(@RestForm String l) {
            i18n.set(l);
            return i18n.get();
        }
    }

    @MessageBundle
    public static interface MyBundle {
        @Message
        String my_greeting();
    }
}
