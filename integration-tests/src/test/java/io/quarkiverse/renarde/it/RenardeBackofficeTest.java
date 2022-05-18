package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import model.ExampleEntity;
import model.ExampleEnum;
import model.OneToOneNotOwningEntity;
import model.OneToOneOwningEntity;

@QuarkusTest
public class RenardeBackofficeTest {

    @Transactional
    @BeforeEach
    public void before() {
        ExampleEntity.deleteAll();
        OneToOneOwningEntity.deleteAll();
        OneToOneNotOwningEntity.deleteAll();
        
        new OneToOneOwningEntity().persist();
        new OneToOneOwningEntity().persist();

        new OneToOneNotOwningEntity().persist();
        new OneToOneNotOwningEntity().persist();
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
        List<Long> oneToOneIds = OneToOneNotOwningEntity.<OneToOneNotOwningEntity>streamAll().map(entity -> entity.id).collect(Collectors.toList());

        given()
                .when().get("/_renarde/backoffice/ExampleEntity/create")
                .then()
                .statusCode(200)
                .body(Matchers.matchesRegex(
                        Pattern.compile(".*<input name=\"primitiveBoolean\"\\s+type=\"checkbox\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"primitiveByte\"\\s+type=\"number\"[^/]+min=\""
                        + Byte.MIN_VALUE + "\"\\s+max=\"" + Byte.MAX_VALUE + "\"\\s+step=\"1.0\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"primitiveShort\"\\s+type=\"number\"[^/]+min=\""
                        + Short.MIN_VALUE + "\"\\s+max=\"" + Short.MAX_VALUE + "\"\\s+step=\"1.0\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"primitiveInt\"\\s+type=\"number\"[^/]+min=\""
                        + Integer.MIN_VALUE + "\"\\s+max=\"" + Integer.MAX_VALUE + "\"\\s+step=\"1.0\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"primitiveLong\"\\s+type=\"number\"[^/]+min=\""
                        + Long.MIN_VALUE + "\"\\s+max=\"" + Long.MAX_VALUE + "\"\\s+step=\"1.0\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern
                        .compile(".*<input name=\"primitiveFloat\"\\s+type=\"number\"\\s+step=\"1.0E-5\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern
                        .compile(".*<input name=\"primitiveDouble\"\\s+type=\"number\"\\s+step=\"1.0E-5\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(
                        ".*<input name=\"primitiveChar\"\\s+type=\"text\"[^/]+minlength=\"1\"\\s+maxlength=\"1\".*",
                        Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"string\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"enumeration\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\"A\">A<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\"B\">B<.*", Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(".*<input name=\"date\"\\s+type=\"datetime-local\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(
                        Pattern.compile(".*<input name=\"localDateTime\"\\s+type=\"datetime-local\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"localDate\"\\s+type=\"date\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"localTime\"\\s+type=\"time\".*", Pattern.DOTALL)))

                .body(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"oneToOneOwning\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\""+oneToOneIds.get(0)+"\">OneToOneNotOwningEntity&lt;"+oneToOneIds.get(0)+"&gt;<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\""+oneToOneIds.get(1)+"\">OneToOneNotOwningEntity&lt;"+oneToOneIds.get(1)+"&gt;<.*", Pattern.DOTALL)))

                .body(Matchers
                        .not(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"oneToOneNotOwning\".*", Pattern.DOTALL))));

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 45);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        given()
                .when()
                .formParam("primitiveBoolean", "on")
                .formParam("primitiveByte", "1")
                .formParam("primitiveShort", "2")
                .formParam("primitiveInt", "3")
                .formParam("primitiveLong", "4")
                .formParam("primitiveFloat", "5")
                .formParam("primitiveDouble", "6")
                .formParam("primitiveChar", "a")
                .formParam("string", "aString")
                .formParam("enumeration", "B")
                .formParam("date", JavaExtensions.htmlNormalised(date))
                .formParam("localDateTime", JavaExtensions.htmlNormalised(localDateTime))
                .formParam("localDate", JavaExtensions.htmlNormalised(localDateTime.toLocalDate()))
                .formParam("localTime", JavaExtensions.htmlNormalised(localDateTime.toLocalTime()))
                .formParam("oneToOneOwning", oneToOneIds.get(0))
                .redirects().follow(false)
                .post("/_renarde/backoffice/ExampleEntity/create")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/create"));

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(true, entity.primitiveBoolean);
        Assertions.assertEquals(1, entity.primitiveByte);
        Assertions.assertEquals(2, entity.primitiveShort);
        Assertions.assertEquals(3, entity.primitiveInt);
        Assertions.assertEquals(4, entity.primitiveLong);
        Assertions.assertEquals(5, entity.primitiveFloat);
        Assertions.assertEquals(6, entity.primitiveDouble);
        Assertions.assertEquals('a', entity.primitiveChar);
        Assertions.assertEquals(date, entity.date);
        Assertions.assertEquals(localDateTime, entity.localDateTime);
        Assertions.assertEquals(localDateTime.toLocalDate(), entity.localDate);
        Assertions.assertEquals(localDateTime.toLocalTime(), entity.localTime);
        Assertions.assertEquals("aString", entity.string);
        Assertions.assertEquals(ExampleEnum.B, entity.enumeration);
        Assertions.assertNotNull(entity.oneToOneOwning);
        Assertions.assertEquals(oneToOneIds.get(0), entity.oneToOneOwning.id);
        Assertions.assertNull(entity.oneToOneNotOwning);
    }

    @Test
    public void testBackofficeExampleEntityCreateWithoutSeconds() {
        Assertions.assertEquals(0, ExampleEntity.count());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 0);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        given()
                .when()
                .formParam("date", new SimpleDateFormat(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS_FORMAT).format(date))
                .formParam("localDateTime", localDateTime.format(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS))
                .formParam("localTime", localDateTime.format(JavaExtensions.HTML_TIME_WITHOUT_SECONDS))
                .redirects().follow(false)
                .log().ifValidationFails()
                .post("/_renarde/backoffice/ExampleEntity/create")
                .then()
                .log().ifValidationFails()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/create"));

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(date, entity.date);
        Assertions.assertEquals(localDateTime, entity.localDateTime);
        Assertions.assertEquals(localDateTime.toLocalTime(), entity.localTime);
    }

    @Test
    public void testBackofficeExampleEntityUpdate() {
        Assertions.assertEquals(0, ExampleEntity.count());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 45);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);
        List<OneToOneNotOwningEntity> oneToOnes = OneToOneNotOwningEntity.listAll();

        ExampleEntity entity = new ExampleEntity();
        entity.primitiveBoolean = true;
        entity.primitiveByte = 1;
        entity.primitiveShort = 2;
        entity.primitiveInt = 3;
        entity.primitiveLong = 4;
        entity.primitiveFloat = 5;
        entity.primitiveDouble = 6;
        entity.primitiveChar = 'a';
        entity.string = "aString";
        entity.date = date;
        entity.localDateTime = localDateTime;
        entity.localDate = localDateTime.toLocalDate();
        entity.localTime = localDateTime.toLocalTime();
        entity.enumeration = ExampleEnum.B;
        entity.oneToOneOwning = oneToOnes.get(0);
        ExampleEntity damnit = entity;
        transact(() -> damnit.persist());

        Assertions.assertEquals(1, ExampleEntity.count());
        entity = ExampleEntity.findAll().firstResult();

        given()
                .when().get("/_renarde/backoffice/ExampleEntity/edit/" + entity.id)
                .then()
                .statusCode(200)
                .body(Matchers.matchesRegex(Pattern
                        .compile(".*<input name=\"primitiveBoolean\"\\s+type=\"checkbox\"[^/]+checked.*", Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(
                                ".*<input name=\"primitiveByte\"\\s+type=\"number\"[^/]+min=\"" + Byte.MIN_VALUE
                                        + "\"\\s+max=\"" + Byte.MAX_VALUE + "\"\\s+step=\"1.0\"\\s+value=\"1\".*",
                                Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(
                                ".*<input name=\"primitiveShort\"\\s+type=\"number\"[^/]+min=\"" + Short.MIN_VALUE
                                        + "\"\\s+max=\"" + Short.MAX_VALUE + "\"\\s+step=\"1.0\"\\s+value=\"2\".*",
                                Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(
                                ".*<input name=\"primitiveInt\"\\s+type=\"number\"[^/]+min=\"" + Integer.MIN_VALUE
                                        + "\"\\s+max=\"" + Integer.MAX_VALUE + "\"\\s+step=\"1.0\"\\s+value=\"3\".*",
                                Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(
                                ".*<input name=\"primitiveLong\"\\s+type=\"number\"[^/]+min=\"" + Long.MIN_VALUE
                                        + "\"\\s+max=\"" + Long.MAX_VALUE + "\"\\s+step=\"1.0\"\\s+value=\"4\".*",
                                Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(
                        ".*<input name=\"primitiveFloat\"\\s+type=\"number\"\\s+step=\"1.0E-5\"\\s+value=\"5.0\".*",
                        Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(
                        ".*<input name=\"primitiveDouble\"\\s+type=\"number\"\\s+step=\"1.0E-5\"\\s+value=\"6.0\".*",
                        Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(
                        ".*<input name=\"primitiveChar\"\\s+type=\"text\"[^/]+minlength=\"1\"\\s+maxlength=\"1\"\\s+value=\"a\".*",
                        Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(".*<input name=\"string\"[^/]+value=\"aString\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"enumeration\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\"A\">A<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+selected\\s+value=\"B\">B<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"date\"\\s+type=\"datetime-local\"[^/]+value=\""
                        + JavaExtensions.htmlNormalised(date) + "\".*", Pattern.DOTALL)))
                .body(Matchers
                        .matchesRegex(Pattern.compile(".*<input name=\"localDateTime\"\\s+type=\"datetime-local\"[^/]+value=\""
                                + JavaExtensions.htmlNormalised(localDateTime) + "\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"localDate\"\\s+type=\"date\"[^/]+value=\""
                        + JavaExtensions.htmlNormalised(localDateTime.toLocalDate()) + "\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<input name=\"localTime\"\\s+type=\"time\"[^/]+value=\""
                        + JavaExtensions.htmlNormalised(localDateTime.toLocalTime()) + "\".*", Pattern.DOTALL)))

                .body(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"oneToOneOwning\".*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+selected\\s+value=\""+oneToOnes.get(0).id+"\">OneToOneNotOwningEntity&lt;"+oneToOnes.get(0).id+"&gt;<.*", Pattern.DOTALL)))
                .body(Matchers.matchesRegex(Pattern.compile(".*<option\\s+value=\""+oneToOnes.get(1).id+"\">OneToOneNotOwningEntity&lt;"+oneToOnes.get(1).id+"&gt;<.*", Pattern.DOTALL)))

                .body(Matchers
                        .not(Matchers.matchesRegex(Pattern.compile(".*<select.*name=\"oneToOneNotOwning\".*", Pattern.DOTALL))));


        LocalDateTime otherLocalDateTime = LocalDateTime.of(1996, 11, 22, 13, 24, 44);
        Instant otherInstant = otherLocalDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date otherDate = Date.from(otherInstant);

        given()
                .when()
                // no default value for unchecked
                //        .formParam("primitiveBoolean", "")
                .formParam("primitiveByte", "11")
                .formParam("primitiveShort", "12")
                .formParam("primitiveInt", "13")
                .formParam("primitiveLong", "14")
                .formParam("primitiveFloat", "15")
                .formParam("primitiveDouble", "16")
                .formParam("primitiveChar", "b")
                .formParam("string", "otherString")
                .formParam("enumeration", "A")
                .formParam("date", JavaExtensions.htmlNormalised(otherDate))
                .formParam("localDateTime", JavaExtensions.htmlNormalised(otherLocalDateTime))
                .formParam("localDate", JavaExtensions.htmlNormalised(otherLocalDateTime.toLocalDate()))
                .formParam("localTime", JavaExtensions.htmlNormalised(otherLocalDateTime.toLocalTime()))
                .formParam("oneToOneOwning", oneToOnes.get(1).id)
                .redirects().follow(false)
                .post("/_renarde/backoffice/ExampleEntity/edit/" + entity.id)
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/edit/" + entity.id));

        Assertions.assertEquals(1, ExampleEntity.count());
        Panache.getEntityManager().clear();
        entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(false, entity.primitiveBoolean);
        Assertions.assertEquals(11, entity.primitiveByte);
        Assertions.assertEquals(12, entity.primitiveShort);
        Assertions.assertEquals(13, entity.primitiveInt);
        Assertions.assertEquals(14, entity.primitiveLong);
        Assertions.assertEquals(15, entity.primitiveFloat);
        Assertions.assertEquals(16, entity.primitiveDouble);
        Assertions.assertEquals('b', entity.primitiveChar);
        Assertions.assertEquals(otherDate, entity.date);
        Assertions.assertEquals(otherLocalDateTime, entity.localDateTime);
        Assertions.assertEquals(otherLocalDateTime.toLocalDate(), entity.localDate);
        Assertions.assertEquals(otherLocalDateTime.toLocalTime(), entity.localTime);
        Assertions.assertEquals("otherString", entity.string);
        Assertions.assertEquals(ExampleEnum.A, entity.enumeration);
        Assertions.assertEquals(oneToOnes.get(1).id, entity.oneToOneOwning.id);
        Assertions.assertNull(entity.oneToOneNotOwning);
    }

    @Test
    public void testBackofficeExampleEntityListAndDelete() {
        Assertions.assertEquals(0, ExampleEntity.count());

        ExampleEntity entity = new ExampleEntity();
        transact(() -> entity.persist());

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity loadedEntity = ExampleEntity.findAll().firstResult();

        given()
                .when().get("/_renarde/backoffice/ExampleEntity/index")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("edit/" + entity.id));

        given()
                .when()
                .redirects().follow(false)
                .post("/_renarde/backoffice/ExampleEntity/delete/" + loadedEntity.id)
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
