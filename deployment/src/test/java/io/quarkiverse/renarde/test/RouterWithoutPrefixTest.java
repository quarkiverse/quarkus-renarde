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
import io.quarkiverse.renarde.router.Router;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class RouterWithoutPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class)
                    .addAsResource(new StringAsset("{uri:MyController.route()}\n{uriabs:MyController.route()}"),
                            "templates/MyController/routes.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testRouteTemplateWithPrefix() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/template").then()
                .statusCode(200)
                .body(Matchers.is("/route\n" + url + "route"));
    }

    @Test
    public void testRouteStringWithPrefix() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/route").then()
                .statusCode(200)
                .body(Matchers.is("/route"));
    }

    @Test
    public void testRouteSeeOtherWithPrefix() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/route-see-other").then()
                .statusCode(303)
                .header("Location", Matchers.is(url + "route"));
    }

    @Test
    public void testRouteRedirectWithPrefix() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/route-redirect").then()
                .statusCode(303)
                .header("Location", Matchers.is(url + "route"));
    }

    @Test
    public void testRouteComposition() {
        String[] urls = new String[] { "/UnannotatedController/unannotatedMethod",
                "/absolute1",
                "/UnannotatedController/relative",
                "/relative/unannotatedMethod",
                "/relative/absolute2",
                "/relative/relative",
                "/really-absolute/unannotatedMethod",
                "/really-absolute/absolute3",
                "/really-absolute/relative",
                "/really-absolute",
                "/unannotatedMethod1",
                "/absolute4",
                "/relative1",
                "/unannotatedMethod2",
                "/absolute5",
                "/relative2",
                "/" };
        RestAssured
                .when()
                .get("/composition").then()
                .statusCode(200)
                .body(Matchers.is(String.join("\n", urls) + "\n"));
        for (String url : urls) {
            RestAssured
                    .when()
                    .get(url).then()
                    .statusCode(200)
                    .body(Matchers.is(url));
        }
    }

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance routes();
        }

        @Path("/template")
        public TemplateInstance template() {
            return Templates.routes();
        }

        @Path("/route")
        public String route() {
            return Router.getURI(MyController::route).toString();
        }

        @Path("/route-see-other")
        public void routeSeeOther() {
            seeOther(Router.getAbsoluteURI(MyController::route));
        }

        @Path("/route-redirect")
        public void routeRedirect() {
            route();
        }

        @Path("/composition")
        public String composition() {
            return Router.getURI(UnannotatedController::unannotatedMethod).toString() + "\n"
                    + Router.getURI(UnannotatedController::absoluteMethod).toString() + "\n"
                    + Router.getURI(UnannotatedController::relativeMethod).toString() + "\n"
                    + Router.getURI(RelativeController::unannotatedMethod).toString() + "\n"
                    + Router.getURI(RelativeController::absoluteMethod).toString() + "\n"
                    + Router.getURI(RelativeController::relativeMethod).toString() + "\n"
                    + Router.getURI(AbsoluteController::unannotatedMethod).toString() + "\n"
                    + Router.getURI(AbsoluteController::absoluteMethod).toString() + "\n"
                    + Router.getURI(AbsoluteController::relativeMethod).toString() + "\n"
                    + Router.getURI(AbsoluteController::emptyMethod).toString() + "\n"
                    + Router.getURI(EmptyController::unannotatedMethod1).toString() + "\n"
                    + Router.getURI(EmptyController::absoluteMethod).toString() + "\n"
                    + Router.getURI(EmptyController::relativeMethod).toString() + "\n"
                    + Router.getURI(SlashController::unannotatedMethod2).toString() + "\n"
                    + Router.getURI(SlashController::absoluteMethod).toString() + "\n"
                    + Router.getURI(SlashController::relativeMethod).toString() + "\n"
                    + Router.getURI(SlashController::rootMethod).toString() + "\n";
        }
    }

    public static class UnannotatedController extends Controller {
        public String unannotatedMethod() {
            return "/UnannotatedController/unannotatedMethod";
        }

        @Path("/absolute1")
        public String absoluteMethod() {
            return "/absolute1";
        }

        @Path("relative")
        public String relativeMethod() {
            return "/UnannotatedController/relative";
        }
    }

    // it's not really relative, it's always absolute
    @Path("relative")
    public static class RelativeController extends Controller {
        public String unannotatedMethod() {
            return "/relative/unannotatedMethod";
        }

        @Path("/absolute2")
        public String absoluteMethod() {
            return "/relative/absolute2";
        }

        @Path("relative")
        public String relativeMethod() {
            return "/relative/relative";
        }
    }

    // it's not really relative, it's always absolute
    @Path("/really-absolute")
    public static class AbsoluteController extends Controller {
        public String unannotatedMethod() {
            return "/really-absolute/unannotatedMethod";
        }

        @Path("/absolute3")
        public String absoluteMethod() {
            return "/really-absolute/absolute3";
        }

        @Path("relative")
        public String relativeMethod() {
            return "/really-absolute/relative";
        }

        @Path("")
        public String emptyMethod() {
            return "/really-absolute";
        }
    }

    @Path("")
    public static class EmptyController extends Controller {
        public String unannotatedMethod1() {
            return "/unannotatedMethod1";
        }

        @Path("/absolute4")
        public String absoluteMethod() {
            return "/absolute4";
        }

        @Path("relative1")
        public String relativeMethod() {
            return "/relative1";
        }
    }

    @Path("/")
    public static class SlashController extends Controller {
        public String unannotatedMethod2() {
            return "/unannotatedMethod2";
        }

        @Path("/absolute5")
        public String absoluteMethod() {
            return "/absolute5";
        }

        @Path("relative2")
        public String relativeMethod() {
            return "/relative2";
        }

        @Path("/")
        public String rootMethod() {
            return "/";
        }
    }
}
