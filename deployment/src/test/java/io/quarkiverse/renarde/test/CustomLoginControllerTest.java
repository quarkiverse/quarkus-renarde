package io.quarkiverse.renarde.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.security.LoginPage;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class CustomLoginControllerTest {

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
                .statusCode(302)
                .header("Location", url + "login");
    }

    @Test
    public void testSuccessfulLoginPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "user")
                .param("password", "secret")
                .post("/login").then()
                .statusCode(303)
                .cookie("QuarkusUser")
                .header("Location", url + "protected");
    }

    @Test
    public void testLoginPageWithInvalidJwt() {

        String token = Jwt.issuer("https://example.com/issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();

        given()
                .when()
                .cookie("QuarkusUser", token)
                .log().ifValidationFails()
                .redirects().follow(false)
                .get("/login")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(Matchers.is("fake login page"));

        // invalid issuer
        token = Jwt.issuer("https://example.com/other-issuer")
                .upn("user")
                .innerSign().encrypt();

        var response = given()
                .redirects().follow(false)
                .when()
                .cookie("QuarkusUser", token)
                .get("/login").then()
                .statusCode(303)
                .extract().response();

        String redirectUrl = response.getHeader("Location");
        assertEquals("http://localhost:8081/login", redirectUrl);

        String quarkusUserCookie = response.headers()
                .getValues("Set-Cookie")
                .stream().filter(c -> c.startsWith("QuarkusUser=")).findFirst().get();
        assertEquals("QuarkusUser=;Version=1;Path=/;Max-Age=0", quarkusUserCookie);
    }

    public static class MyController extends Controller {
        @Inject
        RenardeSecurity security;

        @Authenticated
        @Path("/protected")
        public String prot() {
            return "OK";
        }

        @Path("/unprotected")
        public String unprot() {
            return "OK";
        }

        @LoginPage
        @Path("/login")
        public String login() {
            return "fake login page";
        }

        @POST
        @Path("/login")
        public Response login(@RestForm String username, @RestForm String password) {
            MyUser myUser = new MyUser();
            myUser.username = username;
            myUser.password = password;
            return Response.seeOther(Router.getAbsoluteURI(MyController::prot)).cookie(security.makeUserCookie(myUser)).build();
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
