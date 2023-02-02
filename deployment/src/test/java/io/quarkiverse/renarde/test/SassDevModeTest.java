package io.quarkiverse.renarde.test;

import java.net.URL;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class SassDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource(new StringAsset("$primary-color: #333;"),
                            "resources/_base.scss")
                    .addAsManifestResource(new StringAsset("@use 'base';\n"
                            + "\n"
                            + ".something {\n"
                            + "  color: base.$primary-color;\n"
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
                .body(Matchers.is(".something {\n"
                        + "  color: #333;\n"
                        + "}"));
        config.modifyResourceFile("META-INF/resources/styles.scss", s -> s.replace(".something", ".other"));
        RestAssured
                .when()
                .get("/styles.css").then()
                .statusCode(200)
                .body(Matchers.is(".other {\n"
                        + "  color: #333;\n"
                        + "}"));
    }

}
