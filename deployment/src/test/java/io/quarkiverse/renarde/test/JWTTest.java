package io.quarkiverse.renarde.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

public class JWTTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URI uri;

    @Test
    public void testProtectedPageWithInvalidJwt() throws NoSuchAlgorithmException {
        // canary: valid
        String token = Jwt.issuer("https://quarkus.io/issuer")
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
        token = Jwt.issuer("https://quarkus.io/issuer")
                .upn("user")
                .issuedAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Login expired, you've been logged out");
        // invalid issuer
        token = Jwt.issuer("https://quarkus.io/other-issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Invalid session (bad JWT), you've been logged out");
        // invalid signature
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        token = Jwt.issuer("https://quarkus.io/issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign(kp.getPrivate()).encrypt(kp.getPublic());
        assertRedirectWithMessage(token, "Invalid session (bad signature), you've been logged out");
        // invalid user
        token = Jwt.issuer("https://quarkus.io/issuer")
                .upn("cheesy")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Invalid user: cheesy");
    }

    @Test
    public void testLogoutOnlyInvalidatesPresentCookies() {
        String token = Jwt.issuer("https://quarkus.io/issuer")
                .upn("user")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();

        // Only QuarkusUser in the request
        List<String> setCookies = logoutWithCookies(Map.of("QuarkusUser", token));
        assertCookieNames(setCookies, "QuarkusUser", "_renarde_flash", "csrf-token");
        assertCookieInvalidated(setCookies, "QuarkusUser");

        setCookies = logoutWithCookies(Map.of(
                "QuarkusUser", token,
                "quarkus-redirect-location", "http://localhost:8081/somewhere",
                "q_session", "fake-oidc-session",
                "q_session_google", "fake-oidc-session", // not included because no named OIDC tenants
                "q_session_<default>", "fake-default-tenant")); // q_session_<default> skipped
        assertCookieNames(setCookies, "QuarkusUser", "quarkus-redirect-location", "q_session",
                "_renarde_flash", "csrf-token");
        assertCookieInvalidated(setCookies, "QuarkusUser");
        assertCookieInvalidated(setCookies, "quarkus-redirect-location");
        assertCookieInvalidated(setCookies, "q_session");
    }

    private List<String> logoutWithCookies(Map<String, String> cookies) {
        var request = given().when();
        for (var entry : cookies.entrySet()) {
            request.cookie(entry.getKey(), entry.getValue());
        }
        return request
                .log().ifValidationFails()
                .redirects().follow(false)
                .get("/_renarde/security/logout").then()
                .log().ifValidationFails()
                .statusCode(303)
                .extract().headers()
                .getValues("Set-Cookie");
    }

    private void assertCookieNames(List<String> setCookies, String... expectedNames) {
        Set<String> actual = setCookies.stream()
                .map(c -> c.split("=", 2)[0])
                .collect(Collectors.toSet());
        assertEquals(Set.of(expectedNames), actual);
    }

    private void assertCookieInvalidated(List<String> setCookies, String cookieName) {
        Assertions.assertTrue(setCookies.contains(cookieName + "=;Version=1;Path=/;Max-Age=0"),
                cookieName + " should be invalidated when present in request");
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
                .cookie("quarkus-redirect-location")
                .statusCode(303)
                .extract().response();

        // make sure we have a single redirect target
        Assertions.assertEquals(1, response.getHeaders().getValues("Location").size());

        String quarkusUserCookie = response.headers()
                .getValues("Set-Cookie")
                .stream().filter(c -> c.startsWith("QuarkusUser=")).findFirst().get();

        Assertions.assertEquals("QuarkusUser=;Version=1;Path=/;Max-Age=0", quarkusUserCookie);

        // The redirect cookie should be set with the saved URI, not invalidated (it wasn't in the request)
        String quarkusRedirectCookie = response.headers()
                .getValues("Set-Cookie")
                .stream().filter(c -> c.startsWith("quarkus-redirect-location=")).findFirst().get();

        Assertions.assertEquals("quarkus-redirect-location=\"" + uri + "\";Version=1;Path=/", quarkusRedirectCookie);

        // No OIDC tenant cookies should be invalidated
        Assertions.assertTrue(response.headers()
                .getValues("Set-Cookie")
                .stream().noneMatch(c -> c.startsWith("q_session")),
                "OIDC tenant cookies should not be invalidated when not present in request");

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
