package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import model.ExampleEntity;
import model.ExampleEnum;
import model.ManyToManyNotOwningEntity;
import model.ManyToManyOwningEntity;
import model.ManyToOneEntity;
import model.OneToManyEntity;
import model.OneToOneNotOwningEntity;
import model.OneToOneOwningEntity;

@QuarkusTest
public class RenardeBackofficeTest {

    @Transactional
    @BeforeEach
    public void before() {
        ManyToOneEntity.deleteAll();
        ExampleEntity.deleteAll();
        OneToOneOwningEntity.deleteAll();
        OneToOneNotOwningEntity.deleteAll();
        OneToManyEntity.deleteAll();
        ManyToManyOwningEntity.deleteAll();
        ManyToManyNotOwningEntity.deleteAll();

        new OneToOneOwningEntity().persist();
        new OneToOneOwningEntity().persist();

        new OneToOneNotOwningEntity().persist();
        new OneToOneNotOwningEntity().persist();

        new ManyToOneEntity().persist();
        new ManyToOneEntity().persist();

        new OneToManyEntity().persist();
        new OneToManyEntity().persist();

        new ManyToManyOwningEntity().persist();
        new ManyToManyOwningEntity().persist();

        new ManyToManyNotOwningEntity().persist();
        new ManyToManyNotOwningEntity().persist();
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
        List<Long> oneToOneIds = OneToOneNotOwningEntity.<OneToOneNotOwningEntity> streamAll().map(entity -> entity.id)
                .collect(Collectors.toList());
        List<Long> manyToOneIds = ManyToOneEntity.<ManyToOneEntity> streamAll().map(entity -> entity.id)
                .collect(Collectors.toList());
        List<Long> oneToManyIds = OneToManyEntity.<OneToManyEntity> streamAll().map(entity -> entity.id)
                .collect(Collectors.toList());
        List<Long> manyToManyOwningIds = ManyToManyOwningEntity.<ManyToManyOwningEntity> streamAll().map(entity -> entity.id)
                .collect(Collectors.toList());
        List<Long> manyToManyNotOwningIds = ManyToManyNotOwningEntity.<ManyToManyNotOwningEntity> streamAll()
                .map(entity -> entity.id)
                .collect(Collectors.toList());

        String html = given()
                .when().get("/_renarde/backoffice/ExampleEntity/create")
                .then()
                .statusCode(200)
                .extract().body().asString();
        Document document = Jsoup.parse(html);
        Assertions.assertEquals(1, document.select("input[name='primitiveBoolean'][type='checkbox']").size());
        Assertions.assertEquals(1, document.select(
                "input[name='primitiveByte'][type='number'][min=" + Byte.MIN_VALUE + "][max=" + Byte.MAX_VALUE + "][step=1.0]")
                .size());
        Assertions.assertEquals(1, document.select("input[name='primitiveShort'][type='number'][min=" + Short.MIN_VALUE
                + "][max=" + Short.MAX_VALUE + "][step=1.0]").size());
        Assertions.assertEquals(1, document.select("input[name='primitiveInt'][type='number'][min=" + Integer.MIN_VALUE
                + "][max=" + Integer.MAX_VALUE + "][step=1.0]").size());
        Assertions.assertEquals(1, document.select(
                "input[name='primitiveLong'][type='number'][min=" + Long.MIN_VALUE + "][max=" + Long.MAX_VALUE + "][step=1.0]")
                .size());
        Assertions.assertEquals(1, document.select("input[name='primitiveFloat'][type='number'][step=1.0E-5]").size());
        Assertions.assertEquals(1, document.select("input[name='primitiveDouble'][type='number'][step=1.0E-5]").size());
        Assertions.assertEquals(1,
                document.select("input[name='primitiveChar'][type='text'][minlength=1][maxlength=1]").size());
        Assertions.assertEquals(1, document.select("input[name='string']").size());
        Assertions.assertEquals(1, document.select("input[name='requiredString']").size());
        Assertions.assertEquals("This field is required",
                document.select("input[name='requiredString'] ~ small.form-text").text());
        Assertions.assertEquals(1, document.select("textarea[name='lobString']").size());

        Elements enumeration = document.select("select[name='enumeration']");
        Assertions.assertEquals(1, enumeration.size());
        Assertions.assertEquals("A", enumeration.select("option[value='A']").text());
        Assertions.assertEquals("B", enumeration.select("option[value='B']").text());

        Assertions.assertEquals(1, document.select("input[name='date'][type='datetime-local']").size());
        Assertions.assertEquals(1, document.select("input[name='localDateTime'][type='datetime-local']").size());
        Assertions.assertEquals(1, document.select("input[name='localDate'][type='date']").size());
        Assertions.assertEquals(1, document.select("input[name='localTime'][type='time']").size());

        Elements oneToOneOwning = document.select("select[name='oneToOneOwning']");
        Assertions.assertEquals(1, oneToOneOwning.size());
        Assertions.assertEquals("OneToOneNotOwningEntity<" + oneToOneIds.get(0) + ">",
                oneToOneOwning.select("option[value='" + oneToOneIds.get(0) + "']").text());
        Assertions.assertEquals("OneToOneNotOwningEntity<" + oneToOneIds.get(1) + ">",
                oneToOneOwning.select("option[value='" + oneToOneIds.get(1) + "']").text());

        Elements oneToMany = document.select("select[name='oneToMany'][multiple]");
        Assertions.assertEquals(1, oneToMany.size());
        Assertions.assertEquals("ManyToOneEntity<" + manyToOneIds.get(0) + ">",
                oneToMany.select("option[value='" + manyToOneIds.get(0) + "']").text());
        Assertions.assertEquals("ManyToOneEntity<" + manyToOneIds.get(1) + ">",
                oneToMany.select("option[value='" + manyToOneIds.get(1) + "']").text());

        Elements manyToOne = document.select("select[name='manyToOne']");
        Assertions.assertEquals(1, manyToOne.size());
        Assertions.assertEquals("OneToManyEntity<" + oneToManyIds.get(0) + ">",
                manyToOne.select("option[value='" + oneToManyIds.get(0) + "']").text());
        Assertions.assertEquals("OneToManyEntity<" + oneToManyIds.get(1) + ">",
                manyToOne.select("option[value='" + oneToManyIds.get(1) + "']").text());

        Elements manyToManyOwning = document.select("select[name='manyToManyOwning'][multiple]");
        Assertions.assertEquals(1, manyToManyOwning.size());
        Assertions.assertEquals("ManyToManyNotOwningEntity<" + manyToManyNotOwningIds.get(0) + ">",
                manyToManyOwning.select("option[value='" + manyToManyNotOwningIds.get(0) + "']").text());
        Assertions.assertEquals("ManyToManyNotOwningEntity<" + manyToManyNotOwningIds.get(1) + ">",
                manyToManyOwning.select("option[value='" + manyToManyNotOwningIds.get(1) + "']").text());

        Elements manyToManyNotOwning = document.select("select[name='manyToManyNotOwning'][multiple]");
        Assertions.assertEquals(1, manyToManyNotOwning.size());
        Assertions.assertEquals("ManyToManyOwningEntity<" + manyToManyOwningIds.get(0) + ">",
                manyToManyNotOwning.select("option[value='" + manyToManyOwningIds.get(0) + "']").text());
        Assertions.assertEquals("ManyToManyOwningEntity<" + manyToManyOwningIds.get(1) + ">",
                manyToManyNotOwning.select("option[value='" + manyToManyOwningIds.get(1) + "']").text());

        Assertions.assertEquals(0, document.select("select[name='oneToOneNotOwning']").size());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 45);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .formParam("primitiveBoolean", "on")
                .formParam("primitiveByte", "1")
                .formParam("primitiveShort", "2")
                .formParam("primitiveInt", "3")
                .formParam("primitiveLong", "4")
                .formParam("primitiveFloat", "5")
                .formParam("primitiveDouble", "6")
                .formParam("primitiveChar", "a")
                .formParam("string", "aString")
                .formParam("requiredString", "aString")
                .formParam("lobString", "aString")
                .formParam("enumeration", "B")
                .formParam("date", JavaExtensions.htmlNormalised(date))
                .formParam("localDateTime", JavaExtensions.htmlNormalised(localDateTime))
                .formParam("localDate", JavaExtensions.htmlNormalised(localDateTime.toLocalDate()))
                .formParam("localTime", JavaExtensions.htmlNormalised(localDateTime.toLocalTime()))
                .formParam("oneToOneOwning", oneToOneIds.get(0))
                .formParam("oneToMany", manyToOneIds.get(0))
                .formParam("oneToMany", manyToOneIds.get(1))
                .formParam("manyToOne", oneToManyIds.get(0))
                .formParam("manyToManyOwning", manyToManyNotOwningIds.get(0))
                .formParam("manyToManyOwning", manyToManyNotOwningIds.get(1))
                .formParam("manyToManyNotOwning", manyToManyOwningIds.get(0))
                .formParam("manyToManyNotOwning", manyToManyOwningIds.get(1))
                .formParam("action", "CreateAndCreateAnother")
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
        Assertions.assertEquals("aString", entity.lobString);
        Assertions.assertEquals("aString", entity.requiredString);
        Assertions.assertEquals(ExampleEnum.B, entity.enumeration);
        Assertions.assertNotNull(entity.oneToOneOwning);
        Assertions.assertEquals(oneToOneIds.get(0), entity.oneToOneOwning.id);
        Assertions.assertNull(entity.oneToOneNotOwning);
        Assertions.assertEquals(oneToManyIds.get(0), entity.manyToOne.id);
        Assertions.assertEquals(2, entity.oneToMany.size());
        Assertions.assertTrue(manyToOneIds.contains(entity.oneToMany.get(0).id));
        Assertions.assertTrue(manyToOneIds.contains(entity.oneToMany.get(1).id));
        Assertions.assertEquals(2, entity.manyToManyOwning.size());
        Assertions.assertTrue(manyToManyNotOwningIds.contains(entity.manyToManyOwning.get(0).id));
        Assertions.assertTrue(manyToManyNotOwningIds.contains(entity.manyToManyOwning.get(1).id));
        Assertions.assertEquals(2, entity.manyToManyNotOwning.size());
        Assertions.assertTrue(manyToManyOwningIds.contains(entity.manyToManyNotOwning.get(0).id));
        Assertions.assertTrue(manyToManyOwningIds.contains(entity.manyToManyNotOwning.get(1).id));
    }

    @Test
    public void testBackofficeExampleEntityCreateWithoutSeconds() {
        Assertions.assertEquals(0, ExampleEntity.count());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 0);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .formParam("date", new SimpleDateFormat(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS_FORMAT).format(date))
                .formParam("localDateTime", localDateTime.format(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS))
                .formParam("localTime", localDateTime.format(JavaExtensions.HTML_TIME_WITHOUT_SECONDS))
                .formParam("requiredString", "aString")
                .formParam("action", "CreateAndCreateAnother")
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
    public void testBackofficeExampleEntityLinks() {
        Assertions.assertEquals(0, ExampleEntity.count());

        testBackOfficeLink("create", "CreateAndCreateAnother", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/create"));
        testBackOfficeLink("create", "CreateAndContinueEditing",
                Matchers.containsString("/_renarde/backoffice/ExampleEntity/edit/"));
        testBackOfficeLink("create", "Create", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/index"));

        ExampleEntity entity = ExampleEntity.<ExampleEntity> listAll().get(0);
        testBackOfficeLink("edit/" + entity.id, "Save", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/index"));
        testBackOfficeLink("edit/" + entity.id, "SaveAndContinueEditing",
                Matchers.containsString("/_renarde/backoffice/ExampleEntity/edit/"));
    }

    private void testBackOfficeLink(String uri, String action, Matcher<String> matcher) {
        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("requiredString", "aString")
                .multiPart("action", action)
                .redirects().follow(false)
                .log().ifValidationFails()
                .post("/_renarde/backoffice/ExampleEntity/" + uri)
                .then()
                .log().ifValidationFails()
                .statusCode(303)
                .header("Location", matcher);
    }

    @Test
    public void testBackofficeExampleEntityUpdate() {
        Assertions.assertEquals(0, ExampleEntity.count());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 45);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);
        List<OneToOneNotOwningEntity> oneToOnes = OneToOneNotOwningEntity.listAll();
        List<OneToManyEntity> oneToManys = OneToManyEntity.listAll();
        List<ManyToOneEntity> manyToOnes = ManyToOneEntity.listAll();
        List<ManyToManyOwningEntity> manyToManyOwning = ManyToManyOwningEntity.listAll();
        List<ManyToManyNotOwningEntity> manyToManyNotOwning = ManyToManyNotOwningEntity.listAll();

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
        entity.lobString = "aString";
        entity.requiredString = "aString";
        entity.date = date;
        entity.localDateTime = localDateTime;
        entity.localDate = localDateTime.toLocalDate();
        entity.localTime = localDateTime.toLocalTime();
        entity.enumeration = ExampleEnum.B;
        entity.oneToOneOwning = oneToOnes.get(0);
        entity.manyToOne = oneToManys.get(0);
        entity.manyToManyOwning = new ArrayList<>();
        entity.manyToManyOwning.add(manyToManyNotOwning.get(0));
        entity.manyToManyOwning.add(manyToManyNotOwning.get(1));
        entity.manyToManyNotOwning = new ArrayList<>();
        entity.manyToManyNotOwning.add(manyToManyOwning.get(0));
        entity.manyToManyNotOwning.add(manyToManyOwning.get(1));
        // oneToMany is not owning
        ExampleEntity damnit = entity;
        transact(() -> {
            List<ManyToOneEntity> manyToOnes2 = ManyToOneEntity.listAll();
            List<ManyToManyOwningEntity> manyToManyOwning2 = ManyToManyOwningEntity.listAll();
            // these are owning
            manyToOnes2.get(0).manyToOne = damnit;
            manyToOnes2.get(1).manyToOne = damnit;
            // not owning
            manyToManyOwning2.get(0).manyToMany.add(damnit);
            manyToManyOwning2.get(1).manyToMany.add(damnit);
            damnit.persist();
        });

        Assertions.assertEquals(1, ExampleEntity.count());
        entity = ExampleEntity.findAll().firstResult();

        String html = given()
                .when().get("/_renarde/backoffice/ExampleEntity/edit/" + entity.id)
                .then()
                .statusCode(200)
                .extract().body().asString();
        Document document = Jsoup.parse(html);
        Assertions.assertEquals(1, document.select("input[name='primitiveBoolean'][type='checkbox'][checked]").size());
        Assertions.assertEquals(1, document.select(
                "input[name='primitiveByte'][type='number'][min=" + Byte.MIN_VALUE + "][max=" + Byte.MAX_VALUE
                        + "][step=1.0][value=1]")
                .size());
        Assertions.assertEquals(1, document.select("input[name='primitiveShort'][type='number'][min=" + Short.MIN_VALUE
                + "][max=" + Short.MAX_VALUE + "][step=1.0][value=2]").size());
        Assertions.assertEquals(1, document.select("input[name='primitiveInt'][type='number'][min=" + Integer.MIN_VALUE
                + "][max=" + Integer.MAX_VALUE + "][step=1.0][value=3]").size());
        Assertions.assertEquals(1, document.select(
                "input[name='primitiveLong'][type='number'][min=" + Long.MIN_VALUE + "][max=" + Long.MAX_VALUE
                        + "][step=1.0][value=4]")
                .size());
        Assertions.assertEquals(1,
                document.select("input[name='primitiveFloat'][type='number'][step=1.0E-5][value=5.0]").size());
        Assertions.assertEquals(1,
                document.select("input[name='primitiveDouble'][type='number'][step=1.0E-5][value=6.0]").size());
        Assertions.assertEquals(1,
                document.select("input[name='primitiveChar'][type='text'][minlength=1][maxlength=1][value='a']").size());
        Assertions.assertEquals(1, document.select("input[name='string'][value='aString']").size());
        Assertions.assertEquals(1, document.select("input[name='requiredString'][value='aString']").size());
        Assertions.assertEquals("This field is required",
                document.select("input[name='requiredString'] ~ small.form-text").text());
        Assertions.assertEquals("aString", document.select("textarea[name='lobString']").text());

        Elements enumeration = document.select("select[name='enumeration']");
        Assertions.assertEquals(1, enumeration.size());
        Assertions.assertEquals("A", enumeration.select("option[value='A']").text());
        Assertions.assertEquals("B", enumeration.select("option[value='B'][selected]").text());

        Assertions.assertEquals(1,
                document.select(
                        "input[name='date'][type='datetime-local'][value='" + JavaExtensions.htmlNormalised(date) + "']")
                        .size());
        Assertions.assertEquals(1, document.select("input[name='localDateTime'][type='datetime-local'][value='"
                + JavaExtensions.htmlNormalised(localDateTime) + "']").size());
        Assertions.assertEquals(1, document.select("input[name='localDate'][type='date'][value='"
                + JavaExtensions.htmlNormalised(localDateTime.toLocalDate()) + "']").size());
        Assertions.assertEquals(1, document.select("input[name='localTime'][type='time'][value='"
                + JavaExtensions.htmlNormalised(localDateTime.toLocalTime()) + "']").size());

        Elements oneToOneOwning = document.select("select[name='oneToOneOwning']");
        Assertions.assertEquals(1, oneToOneOwning.size());
        Assertions.assertEquals("OneToOneNotOwningEntity<" + oneToOnes.get(0).id + ">",
                oneToOneOwning.select("option[value='" + oneToOnes.get(0).id + "'][selected]").text());
        Assertions.assertEquals("OneToOneNotOwningEntity<" + oneToOnes.get(1).id + ">",
                oneToOneOwning.select("option[value='" + oneToOnes.get(1).id + "']").text());

        Elements oneToMany = document.select("select[name='oneToMany'][multiple]");
        Assertions.assertEquals(1, oneToMany.size());
        Assertions.assertEquals("ManyToOneEntity<" + manyToOnes.get(0).id + ">",
                oneToMany.select("option[value='" + manyToOnes.get(0).id + "'][selected]").text());
        Assertions.assertEquals("ManyToOneEntity<" + manyToOnes.get(1).id + ">",
                oneToMany.select("option[value='" + manyToOnes.get(1).id + "'][selected]").text());

        Elements manyToOne = document.select("select[name='manyToOne']");
        Assertions.assertEquals(1, manyToOne.size());
        Assertions.assertEquals("OneToManyEntity<" + oneToManys.get(0).id + ">",
                manyToOne.select("option[value='" + oneToManys.get(0).id + "'][selected]").text());
        Assertions.assertEquals("OneToManyEntity<" + oneToManys.get(1).id + ">",
                manyToOne.select("option[value='" + oneToManys.get(1).id + "']").text());

        Elements manyToManyOwningSel = document.select("select[name='manyToManyOwning'][multiple]");
        Assertions.assertEquals(1, manyToManyOwningSel.size());
        Assertions.assertEquals("ManyToManyNotOwningEntity<" + manyToManyNotOwning.get(0).id + ">",
                manyToManyOwningSel.select("option[value='" + manyToManyNotOwning.get(0).id + "'][selected]").text());
        Assertions.assertEquals("ManyToManyNotOwningEntity<" + manyToManyNotOwning.get(1).id + ">",
                manyToManyOwningSel.select("option[value='" + manyToManyNotOwning.get(1).id + "'][selected]").text());

        Elements manyToManyNotOwningSel = document.select("select[name='manyToManyNotOwning'][multiple]");
        Assertions.assertEquals(1, manyToManyNotOwningSel.size());
        Assertions.assertEquals("ManyToManyOwningEntity<" + manyToManyOwning.get(0).id + ">",
                manyToManyNotOwningSel.select("option[value='" + manyToManyOwning.get(0).id + "'][selected]").text());
        Assertions.assertEquals("ManyToManyOwningEntity<" + manyToManyOwning.get(1).id + ">",
                manyToManyNotOwningSel.select("option[value='" + manyToManyOwning.get(1).id + "'][selected]").text());

        Assertions.assertEquals(0, document.select("select[name='oneToOneNotOwning']").size());

        LocalDateTime otherLocalDateTime = LocalDateTime.of(1996, 11, 22, 13, 24, 44);
        Instant otherInstant = otherLocalDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date otherDate = Date.from(otherInstant);

        given()
                .when()
                .contentType(ContentType.MULTIPART)
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
                .formParam("requiredString", "otherString")
                .formParam("lobString", "otherString")
                .formParam("enumeration", "A")
                .formParam("date", JavaExtensions.htmlNormalised(otherDate))
                .formParam("localDateTime", JavaExtensions.htmlNormalised(otherLocalDateTime))
                .formParam("localDate", JavaExtensions.htmlNormalised(otherLocalDateTime.toLocalDate()))
                .formParam("localTime", JavaExtensions.htmlNormalised(otherLocalDateTime.toLocalTime()))
                .formParam("oneToOneOwning", oneToOnes.get(1).id)
                .formParam("manyToOne", oneToManys.get(1).id)
                .formParam("oneToMany", manyToOnes.get(0).id)
                .formParam("manyToManyOwning", manyToManyNotOwning.get(0).id)
                .formParam("manyToManyNotOwning", manyToManyOwning.get(0).id)
                .formParam("action", "SaveAndContinueEditing")
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
        Assertions.assertEquals("otherString", entity.requiredString);
        Assertions.assertEquals("otherString", entity.lobString);
        Assertions.assertEquals(ExampleEnum.A, entity.enumeration);
        Assertions.assertEquals(oneToOnes.get(1).id, entity.oneToOneOwning.id);
        Assertions.assertEquals(oneToManys.get(1).id, entity.manyToOne.id);
        Assertions.assertEquals(1, entity.oneToMany.size());
        Assertions.assertEquals(manyToOnes.get(0).id, entity.oneToMany.get(0).id);
        Assertions.assertNull(entity.oneToOneNotOwning);
        Assertions.assertEquals(1, entity.manyToManyNotOwning.size());
        Assertions.assertEquals(manyToManyOwning.get(0).id, entity.manyToManyNotOwning.get(0).id);
        Assertions.assertEquals(1, entity.manyToManyOwning.size());
        Assertions.assertEquals(manyToManyNotOwning.get(0).id, entity.manyToManyOwning.get(0).id);
    }

    @Test
    public void testBackofficeExampleEntityListAndDelete() {
        Assertions.assertEquals(0, ExampleEntity.count());

        ExampleEntity entity = new ExampleEntity();
        entity.requiredString = "aString";
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
