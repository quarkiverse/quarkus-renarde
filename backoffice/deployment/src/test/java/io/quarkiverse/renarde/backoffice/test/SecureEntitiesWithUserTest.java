package io.quarkiverse.renarde.backoffice.test;

import java.net.URL;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkiverse.renarde.backoffice.BackofficeIndexController;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import jakarta.persistence.Entity;

public class SecureEntitiesWithUserTest {

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
                .statusCode(302)
                .header("Location", url + "_renarde/security/login");
    }

    @Test
    public void testOrderPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .urlEncodingEnabled(false)
                .when()
                .get("/_renarde/backoffice/SecureEntitiesWithUserTest$Order/index").then()
                .statusCode(302)
                .header("Location", url + "_renarde/security/login");
    }

    @Test
    public void testUserPage() {
        RestAssured
                .given()
                .redirects().follow(false)
                .urlEncodingEnabled(false)
                .when()
                .get("/_renarde/backoffice/SecureEntitiesWithUserTest$User/index").then()
                .statusCode(404);
    }

    @Entity
    public static class Order extends PanacheEntity {
    }

    @Entity
    public static class User extends PanacheEntity implements RenardeUserWithPassword {

        @Override
        public Set<String> roles() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String userId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean registered() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String password() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Authenticated
    public static class Orders extends BackofficeController<Order> {
    }

    @Authenticated
    public static class Index extends BackofficeIndexController {
    }
}
