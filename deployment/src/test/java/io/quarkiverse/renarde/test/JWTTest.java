package io.quarkiverse.renarde.test;

import static io.restassured.RestAssured.given;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkiverse.renarde.util.Flash;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

public class JWTTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testProtectedPageWithInvalidJwt() throws NoSuchAlgorithmException {
        // canary: valid
        String token = Jwt.issuer("https://example.com/issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        // valid
        given()
                .when()
                .cookie("QuarkusUser", token)
                .log().ifValidationFails()
                .redirects().follow(false)
                .get("/")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
        // expired
        token = Jwt.issuer("https://example.com/issuer")
                .upn("user")
                .issuedAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Login expired, you've been logged out");
        // invalid issuer
        token = Jwt.issuer("https://example.com/other-issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Invalid session (bad JWT), you've been logged out");
        // invalid signature
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        token = Jwt.issuer("https://example.com/issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign(kp.getPrivate()).encrypt(kp.getPublic());
        assertRedirectWithMessage(token, "Invalid session (bad signature), you've been logged out");
        // invalid user
        token = Jwt.issuer("https://example.com/issuer")
                .upn("cheesy")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Invalid user: cheesy");
    }

    private void assertRedirectWithMessage(String token, String message) {
        // redirect with message
        Response response = given()
                .when()
                .cookie("QuarkusUser", token)
                .log().ifValidationFails()
                .redirects().follow(false)
                .get("/").then()
                .log().ifValidationFails()
                // logout
                .cookie("QuarkusUser")
                .statusCode(303)
                .extract().response();

        String quarkusUserCookie = response.headers()
                .getValues("Set-Cookie")
                .stream().filter(c -> c.startsWith("QuarkusUser=")).findFirst().get();

        Assertions.assertEquals("QuarkusUser=;Version=1;Path=/;Max-Age=0", quarkusUserCookie);

        String flash = response.cookie(Flash.FLASH_COOKIE_NAME);
        Map<String, Object> data = Flash.decodeCookieValue(flash);
        Assertions.assertTrue(data.containsKey("message"));
        Assertions.assertEquals(message, data.get("message"));
    }

    public static class MyController extends ControllerWithUser<MyUser> {
        @Authenticated
        @Path("/")
        public String prot() {
            return "OK: " + getUser();
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
