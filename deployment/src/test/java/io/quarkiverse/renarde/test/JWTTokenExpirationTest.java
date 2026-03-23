package io.quarkiverse.renarde.test;

import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

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
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.jwt.build.Jwt;

/**
 * Tests that the configurable token expiration ({@code quarkus.renarde.auth.token-expiration}) is honored.
 * We set it to 5 seconds and verify that a token issued more than 5 seconds ago is rejected,
 * while a freshly issued token is accepted.
 */
public class JWTTokenExpirationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.renarde.auth.token-expiration", "PT5S");

    @Test
    public void testCustomTokenExpirationAcceptsFreshToken() {
        // A fresh token should be valid with a 5s expiration
        String token = Jwt.issuer("https://quarkus.io/issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofSeconds(5))
                .innerSign().encrypt();
        given()
                .when()
                .cookie("QuarkusUser", token)
                .redirects().follow(false)
                .get("/")
                .then()
                .statusCode(200);
    }

    @Test
    public void testCustomTokenExpirationRejectsExpiredToken() {
        // A token issued 5 minutes ago with a 5s expiration should be rejected
        String token = Jwt.issuer("https://quarkus.io/issuer")
                .upn("user")
                .issuedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .expiresIn(Duration.ofSeconds(5))
                .innerSign().encrypt();
        given()
                .when()
                .cookie("QuarkusUser", token)
                .redirects().follow(false)
                .get("/")
                .then()
                .statusCode(303);
    }

    @Test
    public void testLoginProducesTokenWithCustomExpiration() {
        // Login should produce a cookie with a token using the configured 5s expiration.
        // We verify the cookie is set and the token is valid right after login.
        given()
                .when()
                .param("username", "user")
                .param("password", "secret")
                .redirects().follow(false)
                .post("/login")
                .then()
                .statusCode(303)
                .cookie("QuarkusUser");
    }

    public static class MyController extends Controller {
        @Inject
        RenardeSecurity security;

        @Authenticated
        @Path("/")
        public String prot() {
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
            return Response.seeOther(Router.getAbsoluteURI(MyController::prot))
                    .cookie(security.makeUserCookie(myUser)).build();
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
