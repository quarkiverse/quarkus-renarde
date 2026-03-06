package io.quarkiverse.renarde.test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

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

public class LoginControllerWithPrefixUnscopedCookieTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.http.root-path", "/my-app")
            .overrideConfigKey("quarkus.renarde.auth.scope-cookies-to-root-path", "false");

    @TestHTTPResource
    URL url;

    @Test
    public void testLoginCookiePathIsRoot() {
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
                .anyMatch(h -> h.startsWith("QuarkusUser=") && h.contains("Path=/"));
        // Make sure it's NOT scoped to /my-app
        boolean wrongPath = cookieHeaders.stream()
                .anyMatch(h -> h.startsWith("QuarkusUser=") && h.contains("Path=/my-app"));
        Assertions.assertTrue(found,
                "QuarkusUser cookie should have Path=/ but Set-Cookie headers were: " + cookieHeaders);
        Assertions.assertFalse(wrongPath,
                "QuarkusUser cookie should NOT have Path=/my-app but Set-Cookie headers were: " + cookieHeaders);
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
