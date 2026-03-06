package io.quarkiverse.renarde.test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class LoginControllerWithPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.http.root-path", "/my-app");

    @TestHTTPResource
    URL url;

    @Test
    public void testUnProtectedPage() {
        // RestAssured auto-prepends /my-app (the root path)
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/unprotected").then()
                .statusCode(200)
                .body(Matchers.is("OK"));
    }

    @Test
    public void testProtectedPageWithoutLogin() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/protected").then()
                .statusCode(302)
                .header("Location", url + "_renarde/security/login");
    }

    @Test
    public void testSuccessfulLoginPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "user")
                .param("password", "secret")
                .post("/_renarde/security/login").then()
                .statusCode(303)
                .cookie("QuarkusUser", Matchers.notNullValue())
                .header("Location", url.toString());
    }

    @Test
    public void testSuccessfulLoginPageCookiePath() {
        List<String> cookieHeaders = RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "user")
                .param("password", "secret")
                .post("/_renarde/security/login").then()
                .statusCode(303)
                .extract().headers().getValues("Set-Cookie");
        boolean found = cookieHeaders.stream()
                .anyMatch(h -> h.startsWith("QuarkusUser=") && h.contains("Path=/my-app"));
        Assertions.assertTrue(found,
                "QuarkusUser cookie should have Path=/my-app but Set-Cookie headers were: " + cookieHeaders);
    }

    @Test
    public void testFailedLoginPage() {
        Map<String, String> cookies = RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "unknown")
                .param("password", "secret")
                .post("/_renarde/security/login").then()
                .statusCode(303)
                .header("Location", url + "_renarde/security/login")
                .extract().cookies();
        Assertions.assertNull(cookies.get("QuarkusUser"));
    }

    @Test
    public void testLogoutRedirectsToRootPath() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/_renarde/security/logout").then()
                .statusCode(303)
                .header("Location", url.toString());
    }

    public static class MyController extends Controller {
        @Authenticated
        @Path("/protected")
        public String prot() {
            return "OK";
        }

        @Path("/unprotected")
        public String unprot() {
            return "OK";
        }
    }

    public static class MyUser implements RenardeUserWithPassword {

        String username;
        String password;

        @Override
        public Set<String> roles() {
            return Collections.emptySet();
        }

        @Override
        public String userId() {
            return username;
        }

        @Override
        public boolean registered() {
            return true;
        }

        @Override
        public String password() {
            return password;
        }

    }

    @ApplicationScoped
    public static class MyUserProvider implements RenardeUserProvider {

        @Override
        public RenardeUser findUser(String tenantId, String authId) {
            if (authId.equals("user")) {
                MyUser user = new MyUser();
                user.username = authId;
                user.password = BcryptUtil.bcryptHash("secret");
                return user;
            }
            return null;
        }

    }
}
