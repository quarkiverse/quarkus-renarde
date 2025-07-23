package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.oidc.test.RenardeCookieFilter;
import io.quarkiverse.renarde.test.DisableCSRFFilter;
import io.quarkiverse.renarde.util.Flash;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.matcher.RestAssuredMatchers;
import io.restassured.response.ValidatableResponse;

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
    void testGravatar() {
        given()
                .when().get("/Application/gravatar")
                .then()
                .statusCode(200)
                .body(containsString(
                        "<img src=\"https://www.gravatar.com/avatar/09abd59eb5653a7183ba812b8261f48b?s=200\" alt=\"Gravatar\" aria-label=\"my gravatar\" class=\"foo bar\"/>"))
                .body(containsString("<img src=\"https://www.gravatar.com/avatar/09abd59eb5653a7183ba812b8261f48b\" />"));
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
                        + "\n/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"
                        + "\n/Application/optionalParams?id=42"));
    }

    @Test
    public void testRouterRedirectEndpoint() {
        given()
                .when().get("/Application/routerRedirect")
                .then()
                .statusCode(200)
                .body(is("http://localhost:8081/absolute"
                        + "\nhttp://localhost:8081/Application/index"
                        + "\nhttp://localhost:8081/Application/params/first/42?q=search"
                        + "\nhttp://localhost:8081/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"
                        + "\nhttp://localhost:8081/Application/optionalParams?id=42"));
        given()
                .when().get("/Application/routerRedirect2")
                .then()
                .statusCode(200)
                .body(is("http://localhost:8081/absolute"
                        + "\nhttp://localhost:8081/Application/index"
                        + "\nhttp://localhost:8081/Application/params/first/42?q=search"
                        + "\nhttp://localhost:8081/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"
                        + "\nhttp://localhost:8081/Application/optionalParams?id=42"));
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
                        + "\nhttp://localhost:8081/Application/primitiveParams?b=true&c=a&bite=2&s=3&i=4&l=5&f=6.0&d=7.0"
                        + "\n/Application/optionalParams?id=42"
                        + "\nhttp://localhost:8081/Application/optionalParams?id=42"));
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
                        "Email: Must be a well-formed email address.\n\n\nManual: Required.\n\n\nRequired: Required.\n\n\n"));
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
                .cookie(Flash.FLASH_COOKIE_NAME,
                        RestAssuredMatchers.detailedCookie()
                                .sameSite("Lax")
                                .httpOnly(true)
                                .secured("https".equals(baseURI.getProtocol())))
                .extract().header("Location");
        given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .when()
                .get(uri)
                .then()
                .statusCode(200)
                .body(is(
                        "Email: Must be a well-formed email address.\n\n\nManual: Required.\n\n\nRequired: Required.\n\n\n"));
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

    @Test
    public void testAuthentication() {
        // the redirect cookie is indirectly tested via a success redirection
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .get("/SecureController/hello")
                .then()
                .statusCode(302)
                .header("Location", baseURI + "_renarde/security/login");
        ValidatableResponse response = given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .formParam("username", "FroMage")
                .formParam("password", "1q2w3e")
                .post("/_renarde/security/login")
                .then()
                .statusCode(303)
                .cookie("QuarkusUser",
                        RestAssuredMatchers.detailedCookie()
                                .sameSite("Lax")
                                .httpOnly(true))
                .header("Location", baseURI + "SecureController/hello");
        given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .get("/SecureController/hello")
                .then()
                .statusCode(200)
                .body(is("Hello Security from FroMage"));
    }

    // since we're using renarde-test which installs a CSRF filter, let's make sure we remove it for this test
    @DisableCSRFFilter
    @Test
    public void testCsrf() {
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        String htmlForm = given()
                .filter(cookieFilter)
                .when().get("/Application/csrf")
                .then()
                .statusCode(200)
                .extract().asString();
        String start = "<input type=\"hidden\" name=\"csrf-token\" value=\"";
        int startIndex = htmlForm.indexOf(start);
        Assertions.assertTrue(startIndex > 0, "Failed to find token in form: " + htmlForm);
        int endIndex = htmlForm.indexOf('"', startIndex + start.length() + 1);
        Assertions.assertTrue(endIndex > 0, "Failed to find end of token in form: " + htmlForm);
        String token = htmlForm.substring(startIndex + start.length(), endIndex);
        Assertions.assertTrue(token.length() > 0, "Empty token in form: " + htmlForm);
        // no token, no dice
        given()
                .filter(cookieFilter)
                .when()
                .param("name", "Stef")
                .post("/Application/csrfForm1")
                .then()
                .statusCode(400);
        // token: good
        given()
                .filter(cookieFilter)
                .when()
                .param("csrf-token", token)
                .param("name", "Stef")
                .post("/Application/csrfForm1")
                .then()
                .statusCode(200)
                .body(is("OK: Stef"));
        // no token, no dice
        given()
                .filter(cookieFilter)
                .when()
                .multiPart("name", "Stef")
                .post("/Application/csrfForm2")
                .then()
                .statusCode(400);
        // token: good
        given()
                .filter(cookieFilter)
                .when()
                .multiPart("csrf-token", token)
                .multiPart("name", "Stef")
                .post("/Application/csrfForm2")
                .then()
                .statusCode(200)
                .body(is("OK: Stef"));
        given()
                .filter(cookieFilter)
                .when()
                .param("csrf-token", token)
                .post("/Application/csrfForm3")
                .then()
                .statusCode(200)
                .body(is("OK"));
        given()
                .filter(cookieFilter)
                .when()
                .multiPart("csrf-token", token)
                .post("/Application/csrfForm4")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testLoginFormEndpoint() {
        // without redirect_uri param
        given()
                .when()
                .get("/_renarde/security/login")
                .then()
                .body(Matchers.not(Matchers.containsString("<input type=\"hidden\" name=\"redirect_uri\"")));

        // with redirect_uri param
        String queryParam = URI.create("http://localhost:8080/admin/page?param1=value1&param2=value2").toASCIIString();

        given()
                .when()
                .queryParam("redirect_uri", queryParam)
                .get("/_renarde/security/login")
                .then()
                .body(Matchers
                        .containsString("<input type=\"hidden\" name=\"redirect_uri\" value=\"%s\"/>".formatted(queryParam)));

        // with redirect_uri param that includes a URL as one of its query parameters
        String innerUrl = URLEncoder.encode("http://localhost:8080/other/page?param1=value1&param2=value2",
                StandardCharsets.UTF_8);
        queryParam = "http://localhost:8080/admin/page?url=" + URLEncoder.encode(innerUrl, StandardCharsets.UTF_8);
        given()
                .when()
                .queryParam("redirect_uri", queryParam)
                .get("/_renarde/security/login")
                .then()
                .body(Matchers
                        .containsString("<input type=\"hidden\" name=\"redirect_uri\" value=\"%s\"/>".formatted(queryParam)));
    }
}
