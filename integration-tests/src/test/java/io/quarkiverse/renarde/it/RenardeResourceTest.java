package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RenardeResourceTest {

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
                .contentType(is("text/html"))
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
}
