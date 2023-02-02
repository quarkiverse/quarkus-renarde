package io.quarkiverse.renarde.test;

import java.net.URL;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class SassTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource(new StringAsset("$font-stack: Helvetica, sans-serif;\n"
                            + "$primary-color: #333;\n"
                            + "\n"
                            + "body {\n"
                            + "  font: 100% $font-stack;\n"
                            + "  color: $primary-color;\n"
                            + "}"),
                            "resources/_base.scss")
                    .addAsManifestResource(new StringAsset("@use 'base';\n"
                            + "\n"
                            + ".inverse {\n"
                            + "  background-color: base.$primary-color;\n"
                            + "  color: white;\n"
                            + "}"),
                            "resources/styles.scss"));

    @TestHTTPResource
    URL url;

    @Test
    public void testCss() {
        RestAssured
                .when()
                .get("/styles.css").then()
                .statusCode(200)
                .body(Matchers.is("body {\n"
                        + "  font: 100% Helvetica, sans-serif;\n"
                        + "  color: #333;\n"
                        + "}\n"
                        + "\n"
                        + ".inverse {\n"
                        + "  background-color: #333;\n"
                        + "  color: white;\n"
                        + "}"));
        RestAssured
                .when()
                .get("/base.css").then()
                .statusCode(404);
        RestAssured
                .when()
                .get("/_base.css").then()
                .statusCode(404);
    }

}
