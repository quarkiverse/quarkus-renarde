package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.oidc.test.RenardeCookieFilter;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import model.User;

@QuarkusTest
public class RenardeResourceTest {

    @TestHTTPResource
    URL baseURI;

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/Application/hello")
                .then()
                .statusCode(200)
                .body(is("Hello Renarde"));
    }

    @Test
    public void testParamEndpoint() {
        given()
                .when().get("/Application/params/first/42?q=search")
                .then()
                .statusCode(200)
                .body(is("Got params: first/42/search"));
        given()
                .when()
                .queryParam("b", "true")
                .queryParam("c", "a")
                .queryParam("bite", "2")
                .queryParam("s", "3")
                .queryParam("i", "4")
                .queryParam("l", "5")
                .queryParam("f", "6.0")
                .queryParam("d", "7.0")
                .get("/Application/primitiveParams")
                .then()
                .statusCode(200)
                .body(is("Got params: true/a/2/3/4/5/6.0/7.0"));
    }

    @Test
    public void testTemplateEndpoint() {
        given()
                .when().get("/Application/index")
                .then()
                .statusCode(200)
                .contentType(is("text/html;charset=UTF-8"))
                .body(is("This is my index"));
    }

    @Test
    public void testAbsolutePathEndpoint() {
        given()
                .when().get("/absolute")
                .then()
                .statusCode(200)
                .body(is("Absolute"));
    }

    @Test
    public void testRouterEndpoint() {
        given()
                .when().get("/Application/router")
                .then()
                .statusCode(200)
                .body(is("/absolute"
                        + "\n/Application/index"
                        + "\n/Application/params/first/42?q=search"
                        + "\n/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"));
    }

    @Test
    public void testRoutingTags() {
        given()
                .when().get("/Application/routingTags")
                .then()
                .statusCode(200)
                .body(is("/absolute"
                        + "\nhttp://localhost:8081/absolute"
                        + "\n/Application/index"
                        + "\nhttp://localhost:8081/Application/index"
                        + "\n/Application/params/first/42?q=search"
                        + "\nhttp://localhost:8081/Application/params/first/42?q=search"
                        + "\n/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"
                        + "\nhttp://localhost:8081/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"));
    }

    @Test
    public void testPost() {
        // FIXME: needs more work
        given()
                .when()
                .multiPart("param", "myParam")
                .multiPart("file", "file.txt", "file contents".getBytes(), MediaType.TEXT_PLAIN)
                .multiPart("fileUpload", "fileUpload.txt", "upload file contents".getBytes(), MediaType.TEXT_PLAIN)
                .log().all()
                .post("/Application/form")
                .then()
                .statusCode(200)
                .body(is("param: myParam, file: file contents, fileUpload: fileUpload.txt"));
    }

    @Test
    public void testValidationError() {
        given()
                .redirects().follow(false)
                .when()
                .formParam("redirect", "false")
                // invalid
                .formParam("email", "foobar.com")
                // missing: required, manual
                .post("/Application/validatedAction")
                .then()
                .statusCode(200)
                .body(is(
                        "Email: must be a well-formed email address\n\n\nManual: Required\n\n\nRequired: must not be blank\n\n\n"));
    }

    @Test
    public void testValidationErrorFlash() {
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        String uri = given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .when()
                .formParam("redirect", "true")
                // invalid
                .formParam("email", "foobar.com")
                // missing: required, manual
                .post("/Application/validatedAction")
                .then()
                .statusCode(303)
                .extract().header("Location");
        given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .when()
                .get(uri)
                .then()
                .statusCode(200)
                .body(is(
                        "Email: must be a well-formed email address\n\n\nManual: Required\n\n\nRequired: must not be blank\n\n\n"));
    }

    @Test
    public void testRedirectHook() {
        RestAssuredConfig redirectWithCookiesConfig = RestAssuredConfig.newConfig()
                .httpClient(HttpClientConfig.httpClientConfig().setParam(COOKIE_POLICY, BROWSER_COMPATIBILITY));
        given()
                .config(redirectWithCookiesConfig)
                .when()
                .get("/RedirectHook/redirectHookDirect")
                .then()
                .statusCode(200)
                .body(is("OK"));

        given()
                .config(redirectWithCookiesConfig)
                .when()
                .get("/RedirectHook/redirectHookIndirect")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Transactional
    void setup() {
        User.deleteAll();
        User user = new User();
        user.username = "FroMage";
        user.password = BcryptUtil.bcryptHash("1q2w3e");
        user.persistAndFlush();
    }

    @Test
    public void testAuthentication() {
        setup();
        // the redirect cookie is indirectly tested via a success redirection
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .get("/SecureController/hello")
                .then()
                .statusCode(302)
                .header("Location", baseURI + "_renarde/security/login");
        given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .formParam("username", "FroMage")
                .formParam("password", "1q2w3e")
                .post("/_renarde/security/login")
                .then()
                .statusCode(303)
                .header("Location", baseURI + "SecureController/hello");
        given()
                .filter(cookieFilter)
                .get("/SecureController/hello")
                .then()
                .statusCode(200)
                .body(is("Hello Security from FroMage"));
    }
}
