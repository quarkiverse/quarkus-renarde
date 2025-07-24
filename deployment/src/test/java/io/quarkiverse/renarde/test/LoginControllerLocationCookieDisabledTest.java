package io.quarkiverse.renarde.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class LoginControllerLocationCookieDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyUser.class, MyUserProvider.class, MyController.class)
                    .addAsResource(new StringAsset("quarkus.renarde.auth.redirect.type=query"),
                            "application.properties")
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
        String location = RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/protected").then()
                .statusCode(302)
                .extract().header("Location");

        //     expectedEncoded = http://localhost:8081/_renarde/security/login?redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fprotected
        String expectedEncoded = url + "_renarde/security/login?redirect_uri="
                + URLEncoder.encode(url + "protected", StandardCharsets.UTF_8);
        assertEquals(expectedEncoded, location);

        //     expectedRaw = http://localhost:8081/_renarde/security/login?redirect_uri=http://localhost:8081/protected
        String expectedRaw = url + "_renarde/security/login?redirect_uri=" + url + "protected";
        assertEquals(expectedRaw, URLDecoder.decode(location, StandardCharsets.UTF_8));
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
                .cookie("QuarkusUser")
                .header("Location", url.toString()); // no redirect_uri query param, redirects to root

        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "user")
                .param("password", "secret")
                .param("redirect_uri", "http://localhost:8080/admin/page?param1=value1&param2=value2")
                .post("/_renarde/security/login")
                .then()
                .statusCode(303)
                .cookie("QuarkusUser")
                .header("Location", "http://localhost:8080/admin/page?param1=value1&param2=value2");
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

        ExtractableResponse<Response> response = RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .param("username", "unknown")
                .param("password", "secret")
                .param("redirect_uri", "http://localhost:8080/admin/page?param1=value1&param2=value2")
                .post("/_renarde/security/login").then().log().ifValidationFails()
                .statusCode(303)
                .extract();

        assertEquals(url
                + "_renarde/security/login?redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fadmin%2Fpage%3Fparam1%3Dvalue1%26param2%3Dvalue2",
                response.header("Location"));
        Assertions.assertNull(response.cookies().get("QuarkusUser"));
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
