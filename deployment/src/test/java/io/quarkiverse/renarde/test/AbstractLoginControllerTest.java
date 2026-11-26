package io.quarkiverse.renarde.test;

import java.net.URL;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.LoginPage;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

/**
 * A @LoginPage inherited from an abstract controller is served, and redirected to, under the concrete controller path.
 */
public class AbstractLoginControllerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CustomLoginControllerTest.MyUser.class, CustomLoginControllerTest.MyUserProvider.class,
                            BaseLoginController.class, MyLoginController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testProtectedPageWithoutLogin() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/MyLoginController/prot").then()
                .statusCode(302)
                .header("Location", url + "MyLoginController/login");
    }

    @Test
    public void testLoginPage() {
        RestAssured
                .when()
                .get("/MyLoginController/login").then()
                .statusCode(200)
                .body(Matchers.is("login page"));
    }

    public static abstract class BaseLoginController extends Controller {
        @LoginPage
        public String login() {
            return "login page";
        }
    }

    public static class MyLoginController extends BaseLoginController {
        @Authenticated
        public String prot() {
            return "OK";
        }
    }
}
