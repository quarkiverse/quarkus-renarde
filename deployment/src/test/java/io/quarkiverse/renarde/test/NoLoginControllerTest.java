package io.quarkiverse.renarde.test;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

public class NoLoginControllerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testUnProtectedPage() {
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
                .statusCode(401);
    }

    @Test
    public void testLoginPageIsAbsent() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "user")
                .param("password", "secret")
                .post("/_renarde/security/login").then()
                .statusCode(404);
        // make sure it's not detected by RESTEasy Reactive even without Renarde
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "user")
                .param("password", "secret")
                .post("/_renarde/security").then()
                .statusCode(404);
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/_renarde/security/login").then()
                .statusCode(404);
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

    public static class MyUser implements RenardeUser {

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
