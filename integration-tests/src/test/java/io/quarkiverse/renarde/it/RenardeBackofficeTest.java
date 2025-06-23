package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hibernate.engine.jdbc.BlobProxy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.jpa.NamedBlob;
import io.quarkiverse.renarde.oidc.test.RenardeCookieFilter;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import model.ConstraintedEntity;
import model.CustomIdEntity;
import model.CustomIdOneToManyEntity;
import model.ExampleEntity;
import model.ExampleEnum;
import model.ManyToManyNotOwningEntity;
import model.ManyToManyOwningEntity;
import model.ManyToOneEntity;
import model.OneToManyEntity;
import model.OneToOneNotOwningEntity;
import model.OneToOneOwningEntity;
import model.RelatedConstraintedEntity;

@QuarkusTest
public class RenardeBackofficeTest {

    final static String LOREM_1500b = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce sed diam dolor. Etiam pulvinar consequat odio in aliquam. Vivamus gravida lectus id porta aliquam. Vestibulum facilisis auctor placerat. Praesent semper lobortis lectus quis mollis. Nulla tincidunt laoreet metus, ultricies pretium augue sodales a. Mauris vitae viverra nulla. Aliquam auctor erat vel velit porta, et ultricies massa egestas. Proin turpis mauris, tristique ac orci et, tincidunt euismod urna. Cras at enim vel eros euismod suscipit. Etiam id tempor ligula, sit amet iaculis lacus.\n"
            + "\n"
            + "Nam ac ultricies felis, a semper ligula. Sed nibh eros, vulputate vitae augue ut, bibendum lacinia justo. Duis sed magna at metus lobortis euismod. Cras lacus risus, dignissim non finibus ac, semper varius augue. Aenean porttitor nunc metus, nec molestie sapien volutpat eu. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Interdum et malesuada fames ac ante ipsum primis in faucibus.\n"
            + "\n"
            + "Vivamus risus turpis, eleifend eget massa quis, varius cursus lectus. Vivamus feugiat nisi vel odio pharetra, in convallis risus cursus. Curabitur suscipit magna quam. Duis vel metus ex. Praesent viverra risus nec metus hendrerit semper. Donec pellentesque ante ac nulla tincidunt iaculis. Nunc at arcu eu ante suscipit pretium non et diam. Ut euismod risus a neque dignissim, fringilla feugiat dolor bibendum. Nam at eros dapibus, rutrum lectus id, facilisis odio. Curabitur tristique ex sapien, quis maximus lorem tempor quis. Donec ante nibh nam.";

    @Transactional
    @BeforeEach
    public void before() {
        ConstraintedEntity.deleteAll();
        RelatedConstraintedEntity.deleteAll();
        ManyToOneEntity.deleteAll();
        ExampleEntity.deleteAll();
        OneToOneOwningEntity.deleteAll();
        OneToOneNotOwningEntity.deleteAll();
        OneToManyEntity.deleteAll();
        ManyToManyOwningEntity.deleteAll();
        ManyToManyNotOwningEntity.deleteAll();
        CustomIdEntity.deleteAll();
        CustomIdOneToManyEntity.deleteAll();

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

        new RelatedConstraintedEntity().persist();
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
    public void testBackofficeExampleEntityCreate() throws SQLException {
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
        Assertions.assertEquals(1, document.select("input[name='wrapperBoolean'][type='checkbox']").size());
        Assertions.assertEquals(1, document.select(
                "input[name='primitiveByte'][type='number'][min=" + Byte.MIN_VALUE + "][max=" + Byte.MAX_VALUE + "][step=1.0]")
                .size());
        Assertions.assertEquals(1, document.select("input[name='primitiveShort'][type='number'][min=" + Short.MIN_VALUE
                + "][max=" + Short.MAX_VALUE + "][step=1.0]").size());
        Assertions.assertEquals(1, document.select("input[name='primitiveInt'][type='number'][min=" + Integer.MIN_VALUE
                + "][max=" + Integer.MAX_VALUE + "][step=1.0]").size());
        Assertions.assertEquals(1, document.select("input[name='wrapperInt'][type='number'][min=" + Integer.MIN_VALUE
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
        Assertions.assertEquals("This field is required.",
                document.select("input[name='requiredString'] ~ small.form-text").text());
        Assertions.assertEquals(1, document.select("textarea[name='lobString']").size());
        Assertions.assertEquals(1, document.select("textarea[name='longString1']").size());
        Assertions.assertEquals(1, document.select("textarea[name='longString2']").size());
        Assertions.assertEquals(1, document.select("textarea[name='longString3']").size());
        Assertions.assertEquals(1, document.select("textarea[name='longString4']").size());

        Assertions.assertEquals(1, document.select("textarea[name='jsonRecords']").size());

        Elements enumeration = document.select("select[name='enumeration']");
        Assertions.assertEquals(1, enumeration.size());
        Assertions.assertEquals("A", enumeration.select("option[value='A']").text());
        Assertions.assertEquals("B", enumeration.select("option[value='B']").text());

        Assertions.assertEquals(1, document.select("input[name='date'][type='datetime-local']").size());
        Assertions.assertEquals(1, document.select("input[name='localDateTime'][type='datetime-local']").size());
        Assertions.assertEquals(1, document.select("input[name='localDate'][type='date']").size());
        Assertions.assertEquals(1, document.select("input[name='localTime'][type='time']").size());
        Assertions.assertEquals(1, document.select("input[name='timestamp'][type='datetime-local']").size());

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

        Assertions.assertEquals(1, document.select("input[name='arrayBlob'][type='file']").size());
        Assertions.assertEquals(1, document.select("input[name='sqlBlob'][type='file']").size());
        Assertions.assertEquals(1, document.select("input[name='namedBlob'][type='file']").size());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 45);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        given()
                .when()
                .multiPart("primitiveBoolean", "on")
                .multiPart("wrapperBoolean", "on")
                .multiPart("primitiveByte", "1")
                .multiPart("primitiveShort", "2")
                .multiPart("primitiveInt", "3")
                .multiPart("wrapperInt", "3")
                .multiPart("primitiveLong", "4")
                .multiPart("primitiveFloat", "5")
                .multiPart("primitiveDouble", "6")
                .multiPart("primitiveChar", "a")
                .multiPart("string", "aString")
                .multiPart("requiredString", "aString")
                .multiPart("lobString", "aString")
                .multiPart("longString1", "aString")
                .multiPart("longString2", "aString")
                .multiPart("longString3", "aString")
                .multiPart("longString4", "aString")
                .multiPart("enumeration", "B")
                .multiPart("date", JavaExtensions.htmlNormalised(date))
                .multiPart("timestamp", JavaExtensions.htmlNormalised(localDateTime))
                .multiPart("localDateTime", JavaExtensions.htmlNormalised(localDateTime))
                .multiPart("localDate", JavaExtensions.htmlNormalised(localDateTime.toLocalDate()))
                .multiPart("localTime", JavaExtensions.htmlNormalised(localDateTime.toLocalTime()))
                .multiPart("oneToOneOwning", oneToOneIds.get(0))
                .multiPart("oneToMany", manyToOneIds.get(0))
                .multiPart("oneToMany", manyToOneIds.get(1))
                .multiPart("manyToOne", oneToManyIds.get(0))
                .multiPart("manyToManyOwning", manyToManyNotOwningIds.get(0))
                .multiPart("manyToManyOwning", manyToManyNotOwningIds.get(1))
                .multiPart("manyToManyNotOwning", manyToManyOwningIds.get(0))
                .multiPart("manyToManyNotOwning", manyToManyOwningIds.get(1))
                .multiPart("arrayBlob", "array.txt", "array contents".getBytes(), MediaType.TEXT_PLAIN)
                .multiPart("sqlBlob", "sql.txt", LOREM_1500b.getBytes(), MediaType.TEXT_PLAIN)
                .multiPart("namedBlob", "named.pdf", LOREM_1500b.getBytes(), "application/pdf")
                .multiPart("jsonRecords",
                        "[{\"exampleEnum\": \"A\", \"something\": 1}, {\"exampleEnum\": \"B\", \"something\": 2}]")
                .multiPart("action", "CreateAndCreateAnother")
                .redirects().follow(false)
                .post("/_renarde/backoffice/ExampleEntity/create")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/create"));

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(true, entity.primitiveBoolean);
        Assertions.assertEquals(true, entity.wrapperBoolean);
        Assertions.assertEquals(1, entity.primitiveByte);
        Assertions.assertEquals(2, entity.primitiveShort);
        Assertions.assertEquals(3, entity.primitiveInt);
        Assertions.assertEquals(3, entity.wrapperInt);
        Assertions.assertEquals(4, entity.primitiveLong);
        Assertions.assertEquals(5, entity.primitiveFloat);
        Assertions.assertEquals(6, entity.primitiveDouble);
        Assertions.assertEquals('a', entity.primitiveChar);
        Assertions.assertEquals(date, entity.date);
        Assertions.assertEquals(localDateTime, entity.localDateTime);
        Assertions.assertEquals(localDateTime.toLocalDate(), entity.localDate);
        Assertions.assertEquals(localDateTime.toLocalTime(), entity.localTime);
        Assertions.assertEquals(Timestamp.valueOf(localDateTime), entity.timestamp);
        Assertions.assertEquals("aString", entity.string);
        Assertions.assertEquals("aString", entity.lobString);
        Assertions.assertEquals("aString", entity.longString1);
        Assertions.assertEquals("aString", entity.longString2);
        Assertions.assertEquals("aString", entity.longString3);
        Assertions.assertEquals("aString", entity.longString4);
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
        Assertions.assertNotNull(entity.sqlBlob);
        Assertions.assertArrayEquals(entity.sqlBlob.getBytes(0, (int) entity.sqlBlob.length()), LOREM_1500b.getBytes());
        Assertions.assertNotNull(entity.arrayBlob);
        Assertions.assertArrayEquals(entity.arrayBlob, "array contents".getBytes());
        Assertions.assertNotNull(entity.namedBlob);
        Assertions.assertEquals(entity.namedBlob.name, "named.pdf");
        Assertions.assertEquals(entity.namedBlob.mimeType, "application/pdf");
        Assertions.assertArrayEquals(entity.namedBlob.contents.getBytes(0, (int) entity.sqlBlob.length()),
                LOREM_1500b.getBytes());
        Assertions.assertNotNull(entity.jsonRecords);
        Assertions.assertEquals(2, entity.jsonRecords.size());
        Assertions.assertEquals(ExampleEnum.A, entity.jsonRecords.get(0).getExampleEnum());
        Assertions.assertEquals(1, entity.jsonRecords.get(0).getSomething());
        Assertions.assertEquals(ExampleEnum.B, entity.jsonRecords.get(1).getExampleEnum());
        Assertions.assertEquals(2, entity.jsonRecords.get(1).getSomething());
    }

    @Test
    public void testBackofficeFailedValidation() {
        Assertions.assertEquals(0, ConstraintedEntity.count());
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();

        // we do the redirect manually because RestAssured does not store cookies during redirects
        given()
                .filter(cookieFilter)
                .when()
                .multiPart("urlString", "a")
                .multiPart("lengthString", "a")
                .multiPart("sizeString", "a")
                .multiPart("action", "CreateAndCreateAnother")
                .redirects().follow(false)
                .post("/_renarde/backoffice/ConstraintedEntity/create")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ConstraintedEntity/create"));
        // reload for errors
        String html = given()
                .filter(cookieFilter)
                .when()
                .redirects().follow(false)
                .get("/_renarde/backoffice/ConstraintedEntity/create")
                .then()
                .statusCode(200)
                .extract().body().asString();
        Document document = Jsoup.parse(html);
        System.err.println(html);
        checkInvalidItem(document, "input", "nonNullableString", "This field is required.", "Required.");
        checkInvalidItem(document, "input", "nonEmptyString", "This field is required.", "Required.");
        checkInvalidItem(document, "input", "nonNullString", "This field is required.", "Required.");
        checkInvalidItem(document, "input", "nonBlankString", "This field is required.", "Required.");
        checkInvalidItem(document, "input", "urlString", "This field must be a URL.", "Must be a valid URL.");
        checkInvalidItem(document, "input", "sizeString", "This field must be between 2 and 100 characters.",
                "Size must be between 2 and 100.");
        checkInvalidItem(document, "input", "lengthString", "This field must be between 2 and 100 characters.",
                "Length must be between 2 and 100.");
        checkInvalidItem(document, "select", "relatedEntity", "This field is required.", "Required.");

        Assertions.assertEquals(0, ConstraintedEntity.count());

        RelatedConstraintedEntity relatedEntity = RelatedConstraintedEntity.<RelatedConstraintedEntity> listAll().get(0);

        // now make sure we can create a valid one
        given()
                .filter(cookieFilter)
                .when()
                .multiPart("nonNullableString", "a")
                .multiPart("nonEmptyString", "b")
                .multiPart("nonNullString", "c")
                .multiPart("nonBlankString", "d")
                .multiPart("urlString", "http://example.com")
                .multiPart("lengthString", "abc")
                .multiPart("sizeString", "def")
                .multiPart("relatedEntity", relatedEntity.id)
                .multiPart("action", "CreateAndCreateAnother")
                .redirects().follow(false)
                .post("/_renarde/backoffice/ConstraintedEntity/create")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ConstraintedEntity/create"));

        Assertions.assertEquals(1, ConstraintedEntity.count());
        ConstraintedEntity entity = ConstraintedEntity.<ConstraintedEntity> listAll().get(0);
        Assertions.assertEquals("a", entity.nonNullableString);
        Assertions.assertEquals("b", entity.nonEmptyString);
        Assertions.assertEquals("c", entity.nonNullString);
        Assertions.assertEquals("d", entity.nonBlankString);
        Assertions.assertEquals("http://example.com", entity.urlString);
        Assertions.assertEquals("abc", entity.lengthString);
        Assertions.assertEquals("def", entity.sizeString);
        Assertions.assertEquals(relatedEntity, entity.relatedEntity);
    }

    private void checkInvalidItem(Document document, String type, String name, String help, String invalid) {
        Assertions.assertEquals(1, document.select(type + "[name='" + name + "'][class='form-control is-invalid']").size());
        Assertions.assertEquals(help,
                document.select(type + "[name='" + name + "'] ~ small.form-text").text());
        Assertions.assertEquals(invalid,
                document.select(type + "[name='" + name + "'] ~ span.invalid-feedback").text());

    }

    @Test
    public void testBackofficeCustomIdEntityCreate() throws SQLException {
        Assertions.assertEquals(0, CustomIdEntity.count());
        transact(() -> {
            CustomIdOneToManyEntity rel = new CustomIdOneToManyEntity();
            rel.myid = "a";
            rel.persist();

            rel = new CustomIdOneToManyEntity();
            rel.myid = "b";
            rel.persist();

            CustomIdEntity entity = new CustomIdEntity();
            entity.identifiant = "lolo";
            entity.persist();
        });

        String html = given()
                .when()
                .get("/_renarde/backoffice/CustomIdEntity/create")
                .then()
                .statusCode(200)
                .extract().body().asString();
        Document document = Jsoup.parse(html);
        Elements manyToOne = document.select("select[name='manyToOne']");
        Assertions.assertEquals(1, manyToOne.size());
        Assertions.assertEquals("CustomIdOneToManyEntity<a>",
                manyToOne.select("option[value='a']").text());
        Assertions.assertEquals("CustomIdOneToManyEntity<b>",
                manyToOne.select("option[value='b']").text());

        given()
                .when()
                .multiPart("identifiant", "lala")
                .multiPart("manyToOne", "a")
                .multiPart("action", "CreateAndCreateAnother")
                .redirects().follow(false)
                .post("/_renarde/backoffice/CustomIdEntity/create")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/CustomIdEntity/create"));

        Assertions.assertEquals(2, CustomIdEntity.count());
        CustomIdEntity entity = CustomIdEntity.findById("lala");
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("lala", entity.identifiant);
        Assertions.assertNotNull(entity.manyToOne);
        Assertions.assertEquals("a", entity.manyToOne.myid);

        // now the other way around for the one to many
        html = given()
                .when()
                .get("/_renarde/backoffice/CustomIdOneToManyEntity/create")
                .then()
                .statusCode(200)
                .extract().body().asString();
        document = Jsoup.parse(html);
        Elements oneToMany = document.select("select[name='oneToMany']");
        Assertions.assertEquals(1, oneToMany.size());
        Assertions.assertEquals("CustomIdEntity<lala>",
                oneToMany.select("option[value='lala']").text());
        Assertions.assertEquals("CustomIdEntity<lolo>",
                oneToMany.select("option[value='lolo']").text());

        given()
                .when()
                .multiPart("myid", "c")
                .multiPart("oneToMany", "lala")
                .multiPart("oneToMany", "lolo")
                .multiPart("action", "CreateAndCreateAnother")
                .redirects().follow(false)
                .post("/_renarde/backoffice/CustomIdOneToManyEntity/create")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/CustomIdOneToManyEntity/create"));

        Assertions.assertEquals(3, CustomIdOneToManyEntity.count());
        CustomIdOneToManyEntity relEntity = CustomIdOneToManyEntity.findById("c");
        Assertions.assertNotNull(relEntity);
        Assertions.assertEquals("c", relEntity.myid);
        Assertions.assertNotNull(relEntity.oneToMany);
        Assertions.assertEquals(2, relEntity.oneToMany.size());
        if ("lala".equals(relEntity.oneToMany.get(0).identifiant)) {
            Assertions.assertEquals("lolo", relEntity.oneToMany.get(1).identifiant);
        } else {
            Assertions.assertEquals("lala", relEntity.oneToMany.get(1).identifiant);
            Assertions.assertEquals("lolo", relEntity.oneToMany.get(0).identifiant);
        }
    }

    @Test
    public void testBackofficeExampleEntityCreateWithoutSeconds() {
        Assertions.assertEquals(0, ExampleEntity.count());

        LocalDateTime localDateTime = LocalDateTime.of(1997, 12, 23, 14, 25, 0);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date date = Date.from(instant);

        given()
                .when()
                .multiPart("date", new SimpleDateFormat(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS_FORMAT).format(date))
                .multiPart("localDateTime", localDateTime.format(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS))
                .multiPart("localTime", localDateTime.format(JavaExtensions.HTML_TIME_WITHOUT_SECONDS))
                .multiPart("requiredString", "aString")
                .multiPart("action", "CreateAndCreateAnother")
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

        testBackOfficeLink("ExampleEntity", "create", "CreateAndCreateAnother",
                Matchers.endsWith("/_renarde/backoffice/ExampleEntity/create"));
        testBackOfficeLink("ExampleEntity", "create", "CreateAndContinueEditing",
                Matchers.containsString("/_renarde/backoffice/ExampleEntity/edit/"));
        testBackOfficeLink("ExampleEntity", "create", "Create", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/index"));

        ExampleEntity entity = ExampleEntity.<ExampleEntity> listAll().get(0);
        testBackOfficeLink("ExampleEntity", "edit/" + entity.id, "Save",
                Matchers.endsWith("/_renarde/backoffice/ExampleEntity/index"));
        testBackOfficeLink("ExampleEntity", "edit/" + entity.id, "SaveAndContinueEditing",
                Matchers.containsString("/_renarde/backoffice/ExampleEntity/edit/"));
    }

    @Test
    public void testBackofficeCustomIdEntityLinks() {
        Assertions.assertEquals(0, CustomIdEntity.count());

        testBackOfficeLink("CustomIdEntity", "create", "CreateAndCreateAnother",
                Matchers.endsWith("/_renarde/backoffice/CustomIdEntity/create"), "lala1");
        testBackOfficeLink("CustomIdEntity", "create", "CreateAndContinueEditing",
                Matchers.containsString("/_renarde/backoffice/CustomIdEntity/edit/"), "lala2");
        testBackOfficeLink("CustomIdEntity", "create", "Create", Matchers.endsWith("/_renarde/backoffice/CustomIdEntity/index"),
                "lala3");

        CustomIdEntity entity = CustomIdEntity.<CustomIdEntity> listAll().get(0);
        testBackOfficeLink("CustomIdEntity", "edit/" + entity.identifiant, "Save",
                Matchers.endsWith("/_renarde/backoffice/CustomIdEntity/index"), entity.identifiant);
        testBackOfficeLink("CustomIdEntity", "edit/" + entity.identifiant, "SaveAndContinueEditing",
                Matchers.containsString("/_renarde/backoffice/CustomIdEntity/edit/"), entity.identifiant);
    }

    private void testBackOfficeLink(String entityName, String uri, String action, Matcher<String> matcher) {
        testBackOfficeLink(entityName, uri, action, matcher, null);
    }

    private void testBackOfficeLink(String entityName, String uri, String action, Matcher<String> matcher, String identifiant) {
        RequestSpecification requestSpecification = given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("requiredString", "aString");
        if (identifiant != null) {
            requestSpecification
                    .multiPart("identifiant", identifiant);
        }
        requestSpecification
                .multiPart("action", action)
                .redirects().follow(false)
                .log().ifValidationFails()
                .post("/_renarde/backoffice/" + entityName + "/" + uri)
                .then()
                .log().ifValidationFails()
                .statusCode(303)
                .header("Location", matcher);
    }

    @Test
    public void testBackofficeExampleEntityUpdate() throws SQLException {
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
        entity.wrapperBoolean = true;
        entity.primitiveByte = 1;
        entity.primitiveShort = 2;
        entity.primitiveInt = 3;
        entity.wrapperInt = 3;
        entity.primitiveLong = 4;
        entity.primitiveFloat = 5;
        entity.primitiveDouble = 6;
        entity.primitiveChar = 'a';
        entity.string = "aString";
        entity.lobString = "aString";
        entity.longString1 = "aString";
        entity.longString2 = "aString";
        entity.longString3 = "aString";
        entity.longString4 = "aString";
        entity.requiredString = "aString";
        entity.date = date;
        entity.localDateTime = localDateTime;
        entity.localDate = localDateTime.toLocalDate();
        entity.localTime = localDateTime.toLocalTime();
        entity.timestamp = Timestamp.valueOf(localDateTime);
        entity.enumeration = ExampleEnum.B;
        entity.oneToOneOwning = oneToOnes.get(0);
        entity.manyToOne = oneToManys.get(0);
        entity.manyToManyOwning = new ArrayList<>();
        entity.manyToManyOwning.add(manyToManyNotOwning.get(0));
        entity.manyToManyOwning.add(manyToManyNotOwning.get(1));
        entity.manyToManyNotOwning = new ArrayList<>();
        entity.manyToManyNotOwning.add(manyToManyOwning.get(0));
        entity.manyToManyNotOwning.add(manyToManyOwning.get(1));
        entity.arrayBlob = "not touched".getBytes();
        entity.sqlBlob = BlobProxy.generateProxy("to update".getBytes());
        entity.namedBlob = new NamedBlob();
        entity.namedBlob.name = "named.pdf";
        entity.namedBlob.contents = BlobProxy.generateProxy("to update as well".getBytes());
        entity.namedBlob.mimeType = "application/pdf";
        entity.jsonRecords = Arrays.asList(new ExampleEntity.JsonRecord(ExampleEnum.A, 2));
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
        Assertions.assertEquals(1, document.select("input[name='wrapperBoolean'][type='checkbox'][checked]").size());
        Assertions.assertEquals(1, document.select(
                "input[name='primitiveByte'][type='number'][min=" + Byte.MIN_VALUE + "][max=" + Byte.MAX_VALUE
                        + "][step=1.0][value=1]")
                .size());
        Assertions.assertEquals(1, document.select("input[name='primitiveShort'][type='number'][min=" + Short.MIN_VALUE
                + "][max=" + Short.MAX_VALUE + "][step=1.0][value=2]").size());
        Assertions.assertEquals(1, document.select("input[name='primitiveInt'][type='number'][min=" + Integer.MIN_VALUE
                + "][max=" + Integer.MAX_VALUE + "][step=1.0][value=3]").size());
        Assertions.assertEquals(1, document.select("input[name='wrapperInt'][type='number'][min=" + Integer.MIN_VALUE
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
        Assertions.assertEquals("This field is required.",
                document.select("input[name='requiredString'] ~ small.form-text").text());
        Assertions.assertEquals("aString", document.select("textarea[name='lobString']").text());
        Assertions.assertEquals("aString", document.select("textarea[name='longString1']").text());
        Assertions.assertEquals("aString", document.select("textarea[name='longString2']").text());
        Assertions.assertEquals("aString", document.select("textarea[name='longString3']").text());
        Assertions.assertEquals("aString", document.select("textarea[name='longString4']").text());

        Assertions.assertEquals("[{\"exampleEnum\":\"A\",\"something\":2}]",
                document.select("textarea[name='jsonRecords']").text());

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
        Assertions.assertEquals(1, document.select("input[name='timestamp'][type='datetime-local'][value='"
                + JavaExtensions.htmlNormalised(localDateTime) + "']").size());

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

        Assertions.assertEquals(1, document.select("input[name='arrayBlob'][type='file']").size());
        Assertions.assertEquals(1, document.select("input[name='arrayBlob$unset'][type='checkbox']").size());
        Assertions.assertEquals(1, document.select("a[href='../" + entity.id + "/arrayBlob']").size());
        Assertions.assertEquals(1, document.select("input[name='sqlBlob'][type='file']").size());
        Assertions.assertEquals(1, document.select("input[name='sqlBlob$unset'][type='checkbox']").size());
        Assertions.assertEquals(1, document.select("a[href='../" + entity.id + "/sqlBlob']").size());
        Assertions.assertEquals(1, document.select("input[name='namedBlob'][type='file']").size());
        Assertions.assertEquals(1, document.select("input[name='namedBlob$unset'][type='checkbox']").size());
        Assertions.assertEquals(1, document.select("a[href='../" + entity.id + "/namedBlob']").size());

        LocalDateTime otherLocalDateTime = LocalDateTime.of(1996, 11, 22, 13, 24, 44);
        Instant otherInstant = otherLocalDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date otherDate = Date.from(otherInstant);

        given()
                .when()
                // no default value for unchecked
                //        .multiPart("primitiveBoolean", "")
                .multiPart("primitiveByte", "11")
                .multiPart("primitiveShort", "12")
                .multiPart("primitiveInt", "13")
                .multiPart("wrapperInt", "13")
                .multiPart("primitiveLong", "14")
                .multiPart("primitiveFloat", "15")
                .multiPart("primitiveDouble", "16")
                .multiPart("primitiveChar", "b")
                .multiPart("string", "otherString")
                .multiPart("requiredString", "otherString")
                .multiPart("lobString", "otherString")
                .multiPart("longString1", "otherString")
                .multiPart("longString2", "otherString")
                .multiPart("longString3", "otherString")
                .multiPart("longString4", "otherString")
                .multiPart("enumeration", "A")
                .multiPart("date", JavaExtensions.htmlNormalised(otherDate))
                .multiPart("localDateTime", JavaExtensions.htmlNormalised(otherLocalDateTime))
                .multiPart("localDate", JavaExtensions.htmlNormalised(otherLocalDateTime.toLocalDate()))
                .multiPart("localTime", JavaExtensions.htmlNormalised(otherLocalDateTime.toLocalTime()))
                .multiPart("timestamp", JavaExtensions.htmlNormalised(otherLocalDateTime))
                .multiPart("oneToOneOwning", oneToOnes.get(1).id)
                .multiPart("manyToOne", oneToManys.get(1).id)
                .multiPart("oneToMany", manyToOnes.get(0).id)
                .multiPart("manyToManyOwning", manyToManyNotOwning.get(0).id)
                .multiPart("manyToManyNotOwning", manyToManyOwning.get(0).id)
                // do not send arrayBlob to make sure it's not updated
                .multiPart("sqlBlob", "sql.txt", LOREM_1500b.getBytes(), MediaType.TEXT_PLAIN)
                .multiPart("namedBlob", "named.txt", LOREM_1500b.getBytes(), MediaType.TEXT_PLAIN)
                .multiPart("jsonRecords",
                        "[{\"exampleEnum\": \"A\", \"something\": 1}, {\"exampleEnum\": \"B\", \"something\": 2}]")
                .multiPart("action", "SaveAndContinueEditing")
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
        Assertions.assertEquals(false, entity.wrapperBoolean);
        Assertions.assertEquals(11, entity.primitiveByte);
        Assertions.assertEquals(12, entity.primitiveShort);
        Assertions.assertEquals(13, entity.primitiveInt);
        Assertions.assertEquals(13, entity.wrapperInt);
        Assertions.assertEquals(14, entity.primitiveLong);
        Assertions.assertEquals(15, entity.primitiveFloat);
        Assertions.assertEquals(16, entity.primitiveDouble);
        Assertions.assertEquals('b', entity.primitiveChar);
        Assertions.assertEquals(otherDate, entity.date);
        Assertions.assertEquals(otherLocalDateTime, entity.localDateTime);
        Assertions.assertEquals(otherLocalDateTime.toLocalDate(), entity.localDate);
        Assertions.assertEquals(otherLocalDateTime.toLocalTime(), entity.localTime);
        Assertions.assertEquals(Timestamp.valueOf(otherLocalDateTime), entity.timestamp);
        Assertions.assertEquals("otherString", entity.string);
        Assertions.assertEquals("otherString", entity.requiredString);
        Assertions.assertEquals("otherString", entity.lobString);
        Assertions.assertEquals("otherString", entity.longString1);
        Assertions.assertEquals("otherString", entity.longString2);
        Assertions.assertEquals("otherString", entity.longString3);
        Assertions.assertEquals("otherString", entity.longString4);
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
        // not touched
        Assertions.assertNotNull(entity.arrayBlob);
        Assertions.assertArrayEquals(entity.arrayBlob, "not touched".getBytes());
        // updated
        Assertions.assertNotNull(entity.sqlBlob);
        Assertions.assertArrayEquals(entity.sqlBlob.getBytes(0, (int) entity.sqlBlob.length()), LOREM_1500b.getBytes());
        Assertions.assertNotNull(entity.namedBlob);
        Assertions.assertArrayEquals(entity.namedBlob.contents.getBytes(0, (int) entity.sqlBlob.length()),
                LOREM_1500b.getBytes());
        Assertions.assertEquals(entity.namedBlob.name, "named.txt");
        Assertions.assertEquals(entity.namedBlob.mimeType, "text/plain");

        Assertions.assertNotNull(entity.jsonRecords);
        Assertions.assertEquals(2, entity.jsonRecords.size());
        Assertions.assertEquals(ExampleEnum.A, entity.jsonRecords.get(0).getExampleEnum());
        Assertions.assertEquals(1, entity.jsonRecords.get(0).getSomething());
        Assertions.assertEquals(ExampleEnum.B, entity.jsonRecords.get(1).getExampleEnum());
        Assertions.assertEquals(2, entity.jsonRecords.get(1).getSomething());

        // last round to test unsetting binary fields
        given()
                .when()
                .multiPart("requiredString", "otherString")
                .multiPart("arrayBlob$unset", "on")
                .multiPart("sqlBlob$unset", "on")
                .multiPart("namedBlob$unset", "on")
                .multiPart("action", "SaveAndContinueEditing")
                .redirects().follow(false)
                .post("/_renarde/backoffice/ExampleEntity/edit/" + entity.id)
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/ExampleEntity/edit/" + entity.id));

        Assertions.assertEquals(1, ExampleEntity.count());
        Panache.getEntityManager().clear();
        entity = ExampleEntity.findAll().firstResult();
        Assertions.assertNotNull(entity);
        Assertions.assertNull(entity.arrayBlob);
        Assertions.assertNull(entity.sqlBlob);
        Assertions.assertNull(entity.namedBlob);
    }

    @Test
    public void testBackofficeCustomIdEntityUpdate() throws SQLException {
        Assertions.assertEquals(0, CustomIdEntity.count());
        transact(() -> {
            CustomIdOneToManyEntity rel = new CustomIdOneToManyEntity();
            rel.myid = "a";
            rel.persist();

            rel = new CustomIdOneToManyEntity();
            rel.myid = "b";
            rel.persist();

            CustomIdEntity entity = new CustomIdEntity();
            entity.identifiant = "lala";
            entity.manyToOne = rel;
            entity.persist();

            entity = new CustomIdEntity();
            entity.identifiant = "lolo";
            entity.persist();
        });

        CustomIdEntity entity = CustomIdEntity.findById("lala");
        Assertions.assertNotNull(entity);

        String html = given()
                .when().get("/_renarde/backoffice/CustomIdEntity/edit/" + entity.identifiant)
                .then()
                .statusCode(200)
                .extract().body().asString();
        Document document = Jsoup.parse(html);

        Elements manyToOne = document.select("select[name='manyToOne']");
        Assertions.assertEquals(1, manyToOne.size());
        Assertions.assertEquals("CustomIdOneToManyEntity<b>",
                manyToOne.select("option[value='b'][selected]").text());
        Assertions.assertEquals("CustomIdOneToManyEntity<a>",
                manyToOne.select("option[value='a']").text());

        given()
                .when()
                .multiPart("identifiant", "lala")
                .multiPart("manyToOne", "a")
                .multiPart("action", "SaveAndContinueEditing")
                .redirects().follow(false)
                .post("/_renarde/backoffice/CustomIdEntity/edit/" + entity.identifiant)
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/CustomIdEntity/edit/" + entity.identifiant));

        Assertions.assertEquals(2, CustomIdEntity.count());
        Panache.getEntityManager().clear();
        entity = CustomIdEntity.findById("lala");
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("a", entity.manyToOne.myid);

        // now the one to many
        html = given()
                .when().get("/_renarde/backoffice/CustomIdOneToManyEntity/edit/b")
                .then()
                .statusCode(200)
                .extract().body().asString();
        document = Jsoup.parse(html);

        Elements oneToMany = document.select("select[name='oneToMany']");
        Assertions.assertEquals(1, oneToMany.size());
        Assertions.assertEquals("CustomIdEntity<lala>",
                oneToMany.select("option[value='lala']").text());
        Assertions.assertEquals("CustomIdEntity<lolo>",
                oneToMany.select("option[value='lolo']").text());

        given()
                .when()
                .multiPart("myid", "b")
                .multiPart("oneToMany", "lala")
                .multiPart("oneToMany", "lolo")
                .multiPart("action", "SaveAndContinueEditing")
                .redirects().follow(false)
                .post("/_renarde/backoffice/CustomIdOneToManyEntity/edit/b")
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/CustomIdOneToManyEntity/edit/b"));

        Assertions.assertEquals(2, CustomIdOneToManyEntity.count());
        Panache.getEntityManager().clear();
        CustomIdOneToManyEntity relEntity = CustomIdOneToManyEntity.findById("b");
        Assertions.assertNotNull(relEntity);
        Assertions.assertEquals("b", relEntity.myid);
        Assertions.assertNotNull(relEntity.oneToMany);
        Assertions.assertEquals(2, relEntity.oneToMany.size());
        if ("lala".equals(relEntity.oneToMany.get(0).identifiant)) {
            Assertions.assertEquals("lolo", relEntity.oneToMany.get(1).identifiant);
        } else {
            Assertions.assertEquals("lala", relEntity.oneToMany.get(1).identifiant);
            Assertions.assertEquals("lolo", relEntity.oneToMany.get(0).identifiant);
        }
    }

    @Test
    public void testBackofficeExampleEntityListAndDelete() {
        Assertions.assertEquals(0, ExampleEntity.count());

        ExampleEntity entity = new ExampleEntity();
        entity.requiredString = "aString";
        transact(() -> entity.persist());

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity loadedEntity = ExampleEntity.findAll().firstResult();

        assertListAndDelete("ExampleEntity", loadedEntity.id.toString());

        Panache.getEntityManager().clear();
        Assertions.assertEquals(0, ExampleEntity.count());
    }

    @Test
    public void testBackofficeCustomIdEntityListAndDelete() {
        Assertions.assertEquals(0, CustomIdEntity.count());

        CustomIdEntity entity = new CustomIdEntity();
        entity.identifiant = "lala";
        transact(() -> entity.persist());

        Assertions.assertEquals(1, CustomIdEntity.count());
        CustomIdEntity loadedEntity = CustomIdEntity.findAll().firstResult();

        assertListAndDelete("CustomIdEntity", loadedEntity.identifiant.toString());

        Panache.getEntityManager().clear();
        Assertions.assertEquals(0, CustomIdEntity.count());
    }

    private void assertListAndDelete(String entityName, String id) {
        given()
                .when().get("/_renarde/backoffice/" + entityName + "/index")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("edit/" + id));

        given()
                .when()
                .redirects().follow(false)
                .post("/_renarde/backoffice/" + entityName + "/delete/" + id)
                .then()
                .statusCode(303)
                .header("Location", Matchers.endsWith("/_renarde/backoffice/" + entityName + "/index"));
    }

    @Test
    public void testBackofficeBinaryAccess() throws IOException {
        Assertions.assertEquals(0, ExampleEntity.count());

        ExampleEntity entity = new ExampleEntity();
        entity.requiredString = "aString";
        entity.arrayBlob = "Hello World".getBytes();
        entity.namedBlob = new NamedBlob("fu.txt", "text/plain", "Stef");
        transact(() -> entity.persist());

        Assertions.assertEquals(1, ExampleEntity.count());
        ExampleEntity loadedEntity = ExampleEntity.findAll().firstResult();

        binaryAccessUris1("ExampleEntity", loadedEntity.id.toString());

        byte[] jpgBytes = Files.readAllBytes(Path.of("../docs/modules/ROOT/assets/images/oidc-apple-1.png"));

        transact(() -> {
            ExampleEntity toUpdate = (ExampleEntity) ExampleEntity.listAll().get(0);
            toUpdate.arrayBlob = jpgBytes;
            toUpdate.namedBlob = new NamedBlob("foo.txt", "text/pain", "Steff");
        });

        binaryAccessUris2("ExampleEntity", loadedEntity.id.toString(), jpgBytes);
    }

    @Test
    public void testBackofficeBinaryAccessCustomId() throws IOException {
        Assertions.assertEquals(0, CustomIdEntity.count());

        CustomIdEntity entity = new CustomIdEntity();
        entity.identifiant = "lala";
        entity.arrayBlob = "Hello World".getBytes();
        entity.namedBlob = new NamedBlob("fu.txt", "text/plain", "Stef");
        transact(() -> entity.persist());

        Assertions.assertEquals(1, CustomIdEntity.count());
        CustomIdEntity loadedEntity = CustomIdEntity.findAll().firstResult();

        binaryAccessUris1("CustomIdEntity", loadedEntity.identifiant.toString());

        byte[] jpgBytes = Files.readAllBytes(Path.of("../docs/modules/ROOT/assets/images/oidc-apple-1.png"));

        transact(() -> {
            CustomIdEntity toUpdate = (CustomIdEntity) CustomIdEntity.listAll().get(0);
            toUpdate.arrayBlob = jpgBytes;
            toUpdate.namedBlob = new NamedBlob("foo.txt", "text/pain", "Steff");
        });

        binaryAccessUris2("CustomIdEntity", loadedEntity.identifiant.toString(), jpgBytes);
    }

    private void binaryAccessUris2(String entityName, String id, byte[] jpgBytes) {
        byte[] body = given()
                .when().get("/_renarde/backoffice/" + entityName + "/" + id + "/arrayBlob")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .header("Content-Length", "" + jpgBytes.length)
                .extract().asByteArray();
        Assertions.assertArrayEquals(jpgBytes, body);

        given()
                .when().get("/_renarde/backoffice/" + entityName + "/" + id + "/namedBlob")
                .then()
                .statusCode(200)
                .header("Content-Disposition", "attachment; filename=\"foo.txt\"")
                .contentType("text/pain")
                .body(Matchers.equalTo("Steff"));
    }

    private void binaryAccessUris1(String entityName, String id) {
        given()
                .when().get("/_renarde/backoffice/" + entityName + "/" + id + "/arrayBlob")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(Matchers.equalTo("Hello World"));

        // this one is null
        given()
                .when().get("/_renarde/backoffice/" + entityName + "/" + id + "/sqlBlob")
                .then()
                .statusCode(204);

        given()
                .when().get("/_renarde/backoffice/" + entityName + "/" + id + "/namedBlob")
                .then()
                .statusCode(200)
                .header("Content-Disposition", "attachment; filename=\"fu.txt\"")
                .contentType(ContentType.TEXT)
                .body(Matchers.equalTo("Stef"));
    }

    @Transactional
    void transact(Runnable c) {
        c.run();
    }
}
