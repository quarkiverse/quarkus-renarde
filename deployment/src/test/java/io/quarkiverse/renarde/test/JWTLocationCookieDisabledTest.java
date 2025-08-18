package io.quarkiverse.renarde.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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

public class JWTLocationCookieDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsResource(new StringAsset("quarkus.renarde.auth.redirect.type=query"),
                            "application.properties")
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
                .get("/prot")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(Matchers.startsWith("OK: MyUser[username=user, password=$2a$10"));

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

    private void assertRedirectWithMessage(String token, String message) {
        //     expectedLocation = http://localhost:8081/_renarde/security/login?redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fprot%3Fparam1%3Dvalue1%26url%3Dhttp%253A%252F%252Flocalhost%252Fadmin%252Fpage%253Fkey%253Dvalue
        String expectedLocation = uri + "_renarde/security/login?redirect_uri="
                + URLEncoder.encode(
                        uri + "prot?param1=value1&url="
                                + URLEncoder.encode("http://localhost/admin/page?key=value", StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);

        // redirect with message, with query params
        Response response = given()
                .when()
                .cookie("QuarkusUser", token)
                .log().ifValidationFails()
                .redirects().follow(false)
                .param("param1", "value1")
                .param("url", "http://localhost/admin/page?key=value")
                .get("/prot").then()
                .log().ifValidationFails()
                // logout
                .cookie("QuarkusUser")
                .header("Location", expectedLocation)
                .statusCode(303)
                .extract().response();

        // make sure we have a single redirect target
        assertEquals(1, response.getHeaders().getValues("Location").size());

        String quarkusUserCookie = response.headers()
                .getValues("Set-Cookie")
                .stream().filter(c -> c.startsWith("QuarkusUser=")).findFirst().get();

        assertEquals("QuarkusUser=;Version=1;Path=/;Max-Age=0", quarkusUserCookie);

        String flash = response.cookie(Flash.FLASH_COOKIE_NAME);
        Map<String, Object> data = Flash.decodeCookieValue(flash);
        Assertions.assertTrue(data.containsKey("message"));
        assertEquals(message, data.get("message"));

        // without query params
        expectedLocation = uri + "_renarde/security/login?redirect_uri="
                + URLEncoder.encode(this.uri + "prot", StandardCharsets.UTF_8);
        response = given()
                .when()
                .cookie("QuarkusUser", token)
                .log().ifValidationFails()
                .redirects().follow(false)
                .get("/prot").then()
                .log().ifValidationFails()
                // logout
                .cookie("QuarkusUser")
                .header("Location", expectedLocation)
                .statusCode(303)
                .extract().response();

        // make sure we have a single redirect target
        assertEquals(1, response.getHeaders().getValues("Location").size());

        quarkusUserCookie = response.headers()
                .getValues("Set-Cookie")
                .stream().filter(c -> c.startsWith("QuarkusUser=")).findFirst().get();

        assertEquals("QuarkusUser=;Version=1;Path=/;Max-Age=0", quarkusUserCookie);

        flash = response.cookie(Flash.FLASH_COOKIE_NAME);
        data = Flash.decodeCookieValue(flash);
        Assertions.assertTrue(data.containsKey("message"));
        assertEquals(message, data.get("message"));
    }

    public static class MyController extends ControllerWithUser<MyUser> {
        @Authenticated
        @Path("/prot")
        public String prot() {
            return "OK: " + getUser();
        }
    }

    public static record MyUser(String username, String password) implements RenardeUserWithPassword {
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
                return new MyUser(authId, BcryptUtil.bcryptHash("secret"));
            }
            return null;
        }

    }
}
