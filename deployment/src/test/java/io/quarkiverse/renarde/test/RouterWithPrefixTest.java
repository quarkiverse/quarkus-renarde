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

public class RouterWithPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyController.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/support"), "application.properties")
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
                .body(Matchers.is("/support/route\n" + url + "route"));
    }

    @Test
    public void testRouteStringWithPrefix() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/route").then()
                .statusCode(200)
                .body(Matchers.is("/support/route"));
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
    }
}
