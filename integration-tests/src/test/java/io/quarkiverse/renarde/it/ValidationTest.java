package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ValidationTest {
    @Test
    public void validateOnRenardeEndpointShouldResponse20x() {
        given()
                .when().get("/ValidationController/RenardController")
                .then()
                .statusCode(204);
    }

    @Test
    public void validateOnRestEndpointShouldResponse400() {
        given()
                .when().get("/ValidationController/RestController")
                .then()
                .statusCode(400);
    }
}
