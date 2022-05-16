package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.backoffice.BackUtil;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import model.ExampleEntity;
import model.ExampleEnum;

@QuarkusTest
public class RenardeBackofficeTest {

    @Transactional
    @BeforeEach
    public void before() {
        ExampleEntity.deleteAll();
    }
    
    @Test
    public void testBackofficeAllEntities() {
        given()
                .when().get("/_renarde/backoffice/index")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("ExampleEntity"));
    }

    @Test
    public void testBackofficeExampleEntityIndex() {
        given()
                .when().get("/_renarde/backoffice/ExampleEntity/index")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("ExampleEntity"));
    }

    @Test
    public void testBackofficeExampleEntityCreate() {
        Assertions.assertEquals(0, ExampleEntity.count());
        
        given()
                .when().get("/_renarde/backoffice/ExampleEntity/create")
                .then()
                .statusCode(200)
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"bool\"\\s+type=\"checkbox\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"string\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"enumeration\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\"A\">A<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\"B\">B<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"date\"\\s+type=\"datetime-local\".*", Pattern.DOTALL)))
                ;
        
        Instant instant = LocalDateTime.of(1997, 12, 23, 14, 25, 45).atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);
        
        given()
        .when()
        .formParam("bool", "on")
        .formParam("string", "aString")
        .formParam("enumeration", "B")
        .formParam("date", JavaExtensions.htmlNormalised(date))
        .redirects().follow(false)
        .post("/_renarde/backoffice/ExampleEntity/create")
        .then()
        .statusCode(303)
        .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/create"));

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(true, entity.bool);
        Assertions.assertEquals(date, entity.date);
        Assertions.assertEquals("aString", entity.string);
        Assertions.assertEquals(ExampleEnum.B, entity.enumeration);
    }

    @Test
    public void testBackofficeExampleEntityUpdate() {
        Assertions.assertEquals(0, ExampleEntity.count());

        Instant instant = LocalDateTime.of(1997, 12, 23, 14, 25, 45).atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        ExampleEntity entity = new ExampleEntity();
        entity.bool = true;
        entity.string = "aString";
        entity.date = date;
        entity.enumeration = ExampleEnum.B;
        ExampleEntity damnit = entity;
        transact(() -> damnit.persist());

        Assertions.assertEquals(1, ExampleEntity.count());
        entity = ExampleEntity.findAll().firstResult();

        given()
                .when().get("/_renarde/backoffice/ExampleEntity/edit/"+entity.id)
                .then()
                .statusCode(200)
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"bool\"\\s+type=\"checkbox\"[^/]+checked.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"string\"[^/]+value=\"aString\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"enumeration\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\"A\">A<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+selected\\s+value=\"B\">B<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"date\"\\s+type=\"datetime-local\"[^/]+value=\""+JavaExtensions.htmlNormalised(date)+"\".*", Pattern.DOTALL)))
                ;

        Instant otherInstant = LocalDateTime.of(1996, 11, 22, 13, 24, 44).atZone(ZoneId.systemDefault()).toInstant();
        Date otherDate = Date.from(otherInstant);

        given()
        .when()
        // no default value for unchecked
//        .formParam("bool", "")
        .formParam("string", "otherString")
        .formParam("enumeration", "A")
        .formParam("date", JavaExtensions.htmlNormalised(otherDate))
        .redirects().follow(false)
        .post("/_renarde/backoffice/ExampleEntity/edit/"+entity.id)
        .then()
        .statusCode(303)
        .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/edit/"+entity.id));

        Assertions.assertEquals(1, ExampleEntity.count());
        Panache.getEntityManager().clear();
        entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(false, entity.bool);
        Assertions.assertEquals(otherDate, entity.date);
        Assertions.assertEquals("otherString", entity.string);
        Assertions.assertEquals(ExampleEnum.A, entity.enumeration);
    }

    @Test
    public void testBackofficeExampleEntityDelete() {
        Assertions.assertEquals(0, ExampleEntity.count());


        ExampleEntity entity = new ExampleEntity();
        transact(() -> entity.persist());

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity loadedEntity = ExampleEntity.findAll().firstResult();

        given()
        .when()
        .redirects().follow(false)
        .post("/_renarde/backoffice/ExampleEntity/delete/"+loadedEntity.id)
        .then()
        .statusCode(303)
        .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/index"));

        Panache.getEntityManager().clear();
        Assertions.assertEquals(0, ExampleEntity.count());
    }

    @Transactional
    void transact(Runnable c) {
        c.run();
    }
}
