package io.quarkiverse.renarde.backoffice.test;

import java.net.URL;

import jakarta.persistence.Entity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkiverse.renarde.backoffice.BackofficeIndexController;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class SecureEntitiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Order.class, User.class, Orders.class, Index.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testIndexPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .when()
                .get("/_renarde/backoffice/index").then()
                .statusCode(401);
    }

    @Test
    public void testOrderPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .urlEncodingEnabled(false)
                .when()
                .get("/_renarde/backoffice/SecureEntitiesTest$Order/index").then()
                .statusCode(401);
    }

    @Test
    public void testUserPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .urlEncodingEnabled(false)
                .when()
                .get("/_renarde/backoffice/SecureEntitiesTest$User/index").then()
                .statusCode(404);
    }

    @Entity
    public static class Order extends PanacheEntity {
    }

    @Entity
    public static class User extends PanacheEntity {
    }

    @Authenticated
    public static class Orders extends BackofficeController<Order> {
    }

    @Authenticated
    public static class Index extends BackofficeIndexController {
    }
}
