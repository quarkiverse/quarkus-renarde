package io.quarkiverse.renarde.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests that URI building from Qute templates correctly converts String parameter values
 * to the expected types (UUID, int, long, etc.) without ClassCastException.
 */
// TODO: de mporw na ta valw se ena test mono
public class ParamConversionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TypedController.class, MyStatus.class)
                    .addAsResource(new StringAsset(
                            """
                                    {uri:TypedController.withUuid('550e8400-e29b-41d4-a716-446655440000', '660e8400-e29b-41d4-a716-446655440000')}
                                    {uri:TypedController.withInt('42', '99')}
                                    {uri:TypedController.withLong('123456789', '7')}
                                    {uri:TypedController.withBoolean('true', 'false')}
                                    {uri:TypedController.withDouble('3.14', '1.5')}
                                    {uri:TypedController.withFloat('2.5', '9.81')}
                                    {uri:TypedController.withShort('100', '5')}
                                    {uri:TypedController.withByte('6', '3')}
                                    {uri:TypedController.withChar('7', 'y')}
                                    {uri:TypedController.withBigDecimal('99.99', '123.45')}
                                    {uri:TypedController.withBigInteger('999999999999', '123')}
                                    {uri:TypedController.withLocalDate('2025-06-15', '2025-07-20')}
                                    {uri:TypedController.withLocalDateTime('2025-06-15T10:30:00', '2025-07-20T14:00:00')}
                                    {uri:TypedController.withLocalTime('10:30:00', '14:00:00')}
                                    {uri:TypedController.withInstant('2025-06-15T10:30:00Z', '2025-07-20T14:00:00Z')}
                                    {uri:TypedController.withEnum('ACTIVE', 'INACTIVE')}
                                    {uri:TypedController.withString('hello', 'world')}
                                    {uri:TypedController.withBoxedInt('42', '99')}
                                    {uri:TypedController.withBoxedLong('123456789', '7')}
                                    {uri:TypedController.withBoxedBoolean('true', 'false')}
                                    {uri:TypedController.withBoxedDouble('3.14', '1.5')}
                                    {uri:TypedController.withBoxedFloat('2.5', '9.81')}
                                    {uri:TypedController.withBoxedShort('100', '5')}
                                    {uri:TypedController.withBoxedByte('7', '3')}
                                    {uri:TypedController.withBoxedChar('x', 'y')}
                                    {uri:TypedController.withString('hello world', 'foo&bar=baz')}
                                    {uri:TypedController.withUri('https://example.com/path?foo=bar', 'https://other.com?baz=1&x=2')}
                                    {uri:TypedController.withOptionalQuery('42', '99')}
                                    {uri:TypedController.withOptionalQuery('42')}"""),
                            "templates/TypedController/paramTypes.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testQuteTemplateUriBuilding() {
        String[] expectedUris = {
                // Primitive types
                "/TypedController/withInt/42?q=99",
                "/TypedController/withLong/123456789?q=7",
                "/TypedController/withBoolean/true?q=false",
                "/TypedController/withDouble/3.14?q=1.5",
                "/TypedController/withFloat/2.5?q=9.81",
                "/TypedController/withShort/100?q=5",
                "/TypedController/withByte/6?q=3",
                "/TypedController/withChar/7?q=y",
                // Boxed types
                "/TypedController/withBoxedInt/42?q=99",
                "/TypedController/withBoxedLong/123456789?q=7",
                "/TypedController/withBoxedBoolean/true?q=false",
                "/TypedController/withBoxedDouble/3.14?q=1.5",
                "/TypedController/withBoxedFloat/2.5?q=9.81",
                "/TypedController/withBoxedShort/100?q=5",
                "/TypedController/withBoxedByte/7?q=3",
                "/TypedController/withBoxedChar/x?q=y",
                // Reference types
                "/TypedController/withUuid/550e8400-e29b-41d4-a716-446655440000?q=660e8400-e29b-41d4-a716-446655440000",
                "/TypedController/withString/hello?q=world",
                "/TypedController/withBigDecimal/99.99?q=123.45",
                "/TypedController/withBigInteger/999999999999?q=123",
                "/TypedController/withLocalDate/2025-06-15?q=2025-07-20",
                "/TypedController/withLocalDateTime/2025-06-15T10:30?q=2025-07-20T14%3A00",
                "/TypedController/withLocalTime/10:30?q=14%3A00",
                "/TypedController/withInstant/2025-06-15T10:30:00Z?q=2025-07-20T14%3A00%3A00Z",
                "/TypedController/withEnum/ACTIVE?q=INACTIVE",
                // URL-encoded values: spaces, special chars, URIs with query strings
                "/TypedController/withString/hello%20world?q=foo%26bar%3Dbaz",
                "/TypedController/withUri/https:%2F%2Fexample.com%2Fpath%3Ffoo=bar?q=https%3A%2F%2Fother.com%3Fbaz%3D1%26x%3D2",
                // Optional query param: present and absent
                "/TypedController/withOptionalQuery/42?q=99",
                "/TypedController/withOptionalQuery/42",
        };
        String body = RestAssured.when().get("/param-types").then()
                .statusCode(200)
                .extract().body().asString();
        for (String expectedUri : expectedUris) {
            assertTrue(body.contains(expectedUri), "Expected URI not found: " + expectedUri);
        }
    }

    @Test
    public void testEndpointParamConversion() {
        // Verify that @RestPath and @RestQuery params are properly received and converted
        // Each entry: { url, expectedBody }
        String[][] endpoints = {
                // Primitive types
                { "/TypedController/withInt/42?q=99", "42|99" },
                { "/TypedController/withLong/123456789?q=7", "123456789|7" },
                { "/TypedController/withBoolean/true?q=false", "true|false" },
                { "/TypedController/withDouble/3.14?q=1.5", "3.14|1.5" },
                { "/TypedController/withFloat/2.5?q=9.81", "2.5|9.81" },
                { "/TypedController/withShort/100?q=5", "100|5" },
                { "/TypedController/withByte/7?q=3", "7|3" },
                { "/TypedController/withChar/x?q=y", "x|y" },
                // Boxed types
                { "/TypedController/withBoxedInt/42?q=99", "42|99" },
                { "/TypedController/withBoxedLong/123456789?q=7", "123456789|7" },
                { "/TypedController/withBoxedBoolean/true?q=false", "true|false" },
                { "/TypedController/withBoxedDouble/3.14?q=1.5", "3.14|1.5" },
                { "/TypedController/withBoxedFloat/2.5?q=9.81", "2.5|9.81" },
                { "/TypedController/withBoxedShort/100?q=5", "100|5" },
                { "/TypedController/withBoxedByte/7?q=3", "7|3" },
                { "/TypedController/withBoxedChar/x?q=y", "x|y" },
                // Reference types
                { "/TypedController/withUuid/550e8400-e29b-41d4-a716-446655440000?q=660e8400-e29b-41d4-a716-446655440000",
                        "550e8400-e29b-41d4-a716-446655440000|660e8400-e29b-41d4-a716-446655440000" },
                { "/TypedController/withString/hello?q=world", "hello|world" },
                { "/TypedController/withBigDecimal/99.99?q=123.45", "99.99|123.45" },
                { "/TypedController/withBigInteger/999999999999?q=123", "999999999999|123" },
                { "/TypedController/withLocalDate/2025-06-15?q=2025-07-20", "2025-06-15|2025-07-20" },
                { "/TypedController/withLocalDateTime/2025-06-15T10:30:00?q=2025-07-20T14:00:00",
                        "2025-06-15T10:30|2025-07-20T14:00" },
                { "/TypedController/withLocalTime/10:30:00?q=14:00:00", "10:30|14:00" },
                { "/TypedController/withInstant/2025-06-15T10:30:00Z?q=2025-07-20T14:00:00Z",
                        "2025-06-15T10:30:00Z|2025-07-20T14:00:00Z" },
                { "/TypedController/withEnum/ACTIVE?q=INACTIVE", "ACTIVE|INACTIVE" },
                // URL-encoded values: spaces, special chars, URIs with query strings
                // Use urlEncodingEnabled(false) to prevent RestAssured from double-encoding %XX sequences,
                // so the server receives the properly encoded URL and decodes it as expected.
                { "/TypedController/withString/hello%20world?q=foo%26bar%3Dbaz", "hello world|foo&bar=baz" },
                { "/TypedController/withUri/https:%2F%2Fexample.com%2Fpath%3Ffoo=bar?q=https%3A%2F%2Fother.com%3Fbaz%3D1%26x%3D2",
                        "https://example.com/path?foo=bar|https://other.com?baz=1&x=2" },
                // Optional query param: present and absent
                { "/TypedController/withOptionalQuery/42?q=99", "42|99" },
                { "/TypedController/withOptionalQuery/42", "42|null" },
        };
        for (String[] endpoint : endpoints) {
            RestAssured.given().urlEncodingEnabled(false).when().get(endpoint[0])
                    .then().statusCode(200).body(Matchers.is(endpoint[1]));
        }
    }

    @Test
    public void testRouterGetUri() {
        String body = RestAssured.when().get("/router-uris").then()
                .statusCode(200)
                .extract().body().asString();
        String[] expectedUris = {
                // Primitive types
                "/TypedController/withInt/42?q=99",
                "/TypedController/withLong/123456789?q=7",
                "/TypedController/withBoolean/true?q=false",
                "/TypedController/withDouble/3.14?q=1.5",
                "/TypedController/withFloat/2.5?q=9.81",
                "/TypedController/withShort/100?q=5",
                "/TypedController/withByte/6?q=3",
                "/TypedController/withChar/7?q=y",
                // Boxed types
                "/TypedController/withBoxedInt/42?q=99",
                "/TypedController/withBoxedLong/123456789?q=7",
                "/TypedController/withBoxedBoolean/true?q=false",
                "/TypedController/withBoxedDouble/3.14?q=1.5",
                "/TypedController/withBoxedFloat/2.5?q=9.81",
                "/TypedController/withBoxedShort/100?q=5",
                "/TypedController/withBoxedByte/7?q=3",
                "/TypedController/withBoxedChar/x?q=y",
                // Reference types
                "/TypedController/withUuid/550e8400-e29b-41d4-a716-446655440000?q=660e8400-e29b-41d4-a716-446655440000",
                "/TypedController/withString/hello?q=world",
                "/TypedController/withBigDecimal/99.99?q=123.45",
                "/TypedController/withBigInteger/999999999999?q=123",
                "/TypedController/withLocalDate/2025-06-15?q=2025-07-20",
                "/TypedController/withLocalDateTime/2025-06-15T10:30?q=2025-07-20T14%3A00",
                "/TypedController/withLocalTime/10:30?q=14%3A00",
                "/TypedController/withInstant/2025-06-15T10:30:00Z?q=2025-07-20T14%3A00%3A00Z",
                "/TypedController/withEnum/ACTIVE?q=INACTIVE",
                // URL-encoded values
                "/TypedController/withString/hello%20world?q=foo%26bar%3Dbaz",
                "/TypedController/withUri/https:%2F%2Fexample.com%2Fpath%3Ffoo=bar?q=https%3A%2F%2Fother.com%3Fbaz%3D1%26x%3D2",
                // Optional query param: present and absent
                "/TypedController/withOptionalQuery/42?q=99",
                "/TypedController/withOptionalQuery/42",
        };
        String[] lines = body.split("\n");
        assertEquals(expectedUris.length, lines.length,
                "Expected " + expectedUris.length + " URIs but got " + lines.length);
        for (int i = 0; i < expectedUris.length; i++) {
            assertEquals(expectedUris[i], lines[i], "URI mismatch at line " + i);
        }
    }

    @Test
    public void testRouterGetAbsoluteUri() {
        String body = RestAssured.when().get("/router-absolute-uri").then()
                .statusCode(200)
                .extract().body().asString();
        assertTrue(body.startsWith("http://"), "Absolute URI should start with http:// but was: " + body);
        assertTrue(body.endsWith("/TypedController/withInt/42?q=99"),
                "Absolute URI should end with expected path but was: " + body);
    }

    @Test
    public void testConvertParamEdgeCases() {
        // null returns null for any target type (not a String, passes through early return)
        assertNull(Router.convertParam(null, String.class));
        assertNull(Router.convertParam(null, Integer.class));
        assertNull(Router.convertParam(null, UUID.class));

        // already-typed values pass through unchanged (not a String, no conversion needed)
        Integer intValue = 42;
        assertSame(intValue, Router.convertParam(intValue, Integer.class));

        MyStatus enumValue = MyStatus.ACTIVE;
        assertSame(enumValue, Router.convertParam(enumValue, MyStatus.class));

        // Unknown type (URL.class) — no case in the switch, falls to default, not an enum → returns original String
        assertSame("unknown", Router.convertParam("unknown", URL.class));

        // Empty strings are returned as-is for any type
        assertEquals("", Router.convertParam("", Integer.class));
        assertEquals("", Router.convertParam("", UUID.class));
        assertEquals("", Router.convertParam("", Character.class));
    }

    @Test
    public void testConvertParamValidValues() {
        assertEquals("hello", Router.convertParam("hello", String.class));
        // Boxed types
        assertEquals(42, Router.convertParam("42", Integer.class));
        assertEquals(123456789L, Router.convertParam("123456789", Long.class));
        assertEquals(Boolean.TRUE, Router.convertParam("true", Boolean.class));
        assertEquals(Boolean.FALSE, Router.convertParam("false", Boolean.class));
        assertEquals(3.14, Router.convertParam("3.14", Double.class));
        assertEquals(2.5f, Router.convertParam("2.5", Float.class));
        assertEquals((short) 100, Router.convertParam("100", Short.class));
        assertEquals((byte) 6, Router.convertParam("6", Byte.class));
        assertEquals('7', Router.convertParam("7", Character.class));
        // Primitive types
        assertEquals(42, Router.convertParam("42", int.class));
        assertEquals(123456789L, Router.convertParam("123456789", long.class));
        assertEquals(Boolean.TRUE, Router.convertParam("true", boolean.class));
        assertEquals(Boolean.FALSE, Router.convertParam("false", boolean.class));
        assertEquals(3.14, Router.convertParam("3.14", double.class));
        assertEquals(2.5f, Router.convertParam("2.5", float.class));
        assertEquals((short) 100, Router.convertParam("100", short.class));
        assertEquals((byte) 6, Router.convertParam("6", byte.class));
        assertEquals('7', Router.convertParam("7", char.class));
        // Reference types
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                Router.convertParam("550e8400-e29b-41d4-a716-446655440000", UUID.class));
        assertEquals(new BigDecimal("99.99"), Router.convertParam("99.99", BigDecimal.class));
        assertEquals(new BigInteger("999999999999"), Router.convertParam("999999999999", BigInteger.class));
        assertEquals(LocalDate.of(2025, 6, 15), Router.convertParam("2025-06-15", LocalDate.class));
        assertEquals(LocalDateTime.of(2025, 6, 15, 10, 30),
                Router.convertParam("2025-06-15T10:30:00", LocalDateTime.class));
        assertEquals(LocalTime.of(10, 30), Router.convertParam("10:30:00", LocalTime.class));
        assertEquals(Instant.parse("2025-06-15T10:30:00Z"),
                Router.convertParam("2025-06-15T10:30:00Z", Instant.class));
        assertEquals(URI.create("https://example.com/path"),
                Router.convertParam("https://example.com/path", java.net.URI.class));
        // Additional URI variations (fragments, relative paths) are only tested here as unit tests.
        // testQuteTemplateUriBuilding covers URI with query strings in a full integration test.
        assertEquals(URI.create("https://example.com/path?foo=bar&baz=1"),
                Router.convertParam("https://example.com/path?foo=bar&baz=1", java.net.URI.class));
        assertEquals(URI.create("https://example.com/path?q=hello%20world"),
                Router.convertParam("https://example.com/path?q=hello%20world", java.net.URI.class));
        assertEquals(URI.create("https://example.com/path#fragment"),
                Router.convertParam("https://example.com/path#fragment", java.net.URI.class));
        assertEquals(URI.create("/relative/path?key=value"),
                Router.convertParam("/relative/path?key=value", java.net.URI.class));
        assertEquals(MyStatus.ACTIVE, Router.convertParam("ACTIVE", MyStatus.class));
        assertEquals(MyStatus.INACTIVE, Router.convertParam("INACTIVE", MyStatus.class));
    }

    @Test
    public void testConvertParamInvalidValues() {
        // Boxed types
        assertEquals("For input string: \"notANumber\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notANumber", Integer.class))
                        .getMessage());
        assertEquals("For input string: \"notANumber\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notANumber", Long.class))
                        .getMessage());
        // Boolean.valueOf never throws — any non-"true" string returns false
        assertEquals(Boolean.FALSE, Router.convertParam("notTrue", Boolean.class));
        assertEquals("For input string: \"notADouble\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notADouble", Double.class))
                        .getMessage());
        assertEquals("For input string: \"notAFloat\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notAFloat", Float.class))
                        .getMessage());
        assertEquals("For input string: \"notAShort\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notAShort", Short.class))
                        .getMessage());
        assertEquals("For input string: \"notAByte\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notAByte", Byte.class))
                        .getMessage());
        assertEquals("Expected a single character but got: 'hello'",
                assertThrows(IllegalArgumentException.class, () -> Router.convertParam("hello", Character.class))
                        .getMessage());
        // Primitive types
        assertEquals("For input string: \"notANumber\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notANumber", int.class))
                        .getMessage());
        assertEquals("For input string: \"notANumber\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notANumber", long.class))
                        .getMessage());
        assertEquals(Boolean.FALSE, Router.convertParam("notTrue", boolean.class));
        assertEquals("For input string: \"notADouble\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notADouble", double.class))
                        .getMessage());
        assertEquals("For input string: \"notAFloat\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notAFloat", float.class))
                        .getMessage());
        assertEquals("For input string: \"notAShort\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notAShort", short.class))
                        .getMessage());
        assertEquals("For input string: \"notAByte\"",
                assertThrows(NumberFormatException.class, () -> Router.convertParam("notAByte", byte.class))
                        .getMessage());
        assertEquals("Expected a single character but got: 'hello'",
                assertThrows(IllegalArgumentException.class, () -> Router.convertParam("hello", char.class))
                        .getMessage());
        // Reference types
        assertThrows(IllegalArgumentException.class, () -> Router.convertParam("not-a-uuid", UUID.class));
        assertThrows(NumberFormatException.class, () -> Router.convertParam("notADecimal", BigDecimal.class));
        assertThrows(NumberFormatException.class, () -> Router.convertParam("notAnInteger", BigInteger.class));
        assertThrows(Exception.class, () -> Router.convertParam("not-a-date", LocalDate.class));
        assertThrows(Exception.class, () -> Router.convertParam("not-a-datetime", LocalDateTime.class));
        assertThrows(Exception.class, () -> Router.convertParam("not-a-time", LocalTime.class));
        assertThrows(Exception.class, () -> Router.convertParam("not-an-instant", Instant.class));
        assertThrows(IllegalArgumentException.class,
                () -> Router.convertParam("not a valid uri with spaces", java.net.URI.class));
        assertEquals("No enum constant io.quarkiverse.renarde.test.ParamConversionTest.MyStatus.NONEXISTENT",
                assertThrows(IllegalArgumentException.class, () -> Router.convertParam("NONEXISTENT", MyStatus.class))
                        .getMessage());
    }

    public enum MyStatus {
        ACTIVE,
        INACTIVE
    }

    public static class TypedController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance paramTypes();
        }

        @Path("/param-types")
        public TemplateInstance paramTypes() {
            return Templates.paramTypes();
        }

        public String withUuid(@RestPath UUID id, @RestQuery UUID q) {
            return id + "|" + q;
        }

        public String withInt(@RestPath int id, @RestQuery int q) {
            return id + "|" + q;
        }

        public String withLong(@RestPath long id, @RestQuery long q) {
            return id + "|" + q;
        }

        public String withBoolean(@RestPath boolean id, @RestQuery boolean q) {
            return id + "|" + q;
        }

        public String withDouble(@RestPath double id, @RestQuery double q) {
            return id + "|" + q;
        }

        public String withFloat(@RestPath float id, @RestQuery float q) {
            return id + "|" + q;
        }

        public String withShort(@RestPath short id, @RestQuery short q) {
            return id + "|" + q;
        }

        public String withByte(@RestPath byte id, @RestQuery byte q) {
            return id + "|" + q;
        }

        public String withChar(@RestPath char id, @RestQuery char q) {
            return id + "|" + q;
        }

        public String withBigDecimal(@RestPath BigDecimal id, @RestQuery BigDecimal q) {
            return id + "|" + q;
        }

        public String withBigInteger(@RestPath BigInteger id, @RestQuery BigInteger q) {
            return id + "|" + q;
        }

        public String withLocalDate(@RestPath LocalDate id, @RestQuery LocalDate q) {
            return id + "|" + q;
        }

        public String withLocalDateTime(@RestPath LocalDateTime id, @RestQuery LocalDateTime q) {
            return id + "|" + q;
        }

        public String withLocalTime(@RestPath LocalTime id, @RestQuery LocalTime q) {
            return id + "|" + q;
        }

        public String withInstant(@RestPath Instant id, @RestQuery Instant q) {
            return id + "|" + q;
        }

        public String withEnum(@RestPath MyStatus id, @RestQuery MyStatus q) {
            return id + "|" + q;
        }

        public String withString(@RestPath String id, @RestQuery String q) {
            return id + "|" + q;
        }

        public String withBoxedInt(@RestPath Integer id, @RestQuery Integer q) {
            return id + "|" + q;
        }

        public String withBoxedLong(@RestPath Long id, @RestQuery Long q) {
            return id + "|" + q;
        }

        public String withBoxedBoolean(@RestPath Boolean id, @RestQuery Boolean q) {
            return id + "|" + q;
        }

        public String withBoxedDouble(@RestPath Double id, @RestQuery Double q) {
            return id + "|" + q;
        }

        public String withBoxedFloat(@RestPath Float id, @RestQuery Float q) {
            return id + "|" + q;
        }

        public String withBoxedShort(@RestPath Short id, @RestQuery Short q) {
            return id + "|" + q;
        }

        public String withBoxedByte(@RestPath Byte id, @RestQuery Byte q) {
            return id + "|" + q;
        }

        public String withBoxedChar(@RestPath Character id, @RestQuery Character q) {
            return id + "|" + q;
        }

        public String withUri(@RestPath URI id, @RestQuery URI q) {
            return id + "|" + q;
        }

        public String withOptionalQuery(@RestPath int id, @RestQuery Optional<Integer> q) {
            return id + "|" + q.orElse(null);
        }

        @Path("/router-uris")
        public String routerUris() {
            return String.join("\n",
                    // Primitive types
                    Router.getURI(TypedController::withInt, 42, 99).toString(),
                    Router.getURI(TypedController::withLong, 123456789L, 7L).toString(),
                    Router.getURI(TypedController::withBoolean, true, false).toString(),
                    Router.getURI(TypedController::withDouble, 3.14, 1.5).toString(),
                    Router.getURI(TypedController::withFloat, 2.5f, 9.81f).toString(),
                    Router.getURI(TypedController::withShort, (short) 100, (short) 5).toString(),
                    Router.getURI(TypedController::withByte, (byte) 6, (byte) 3).toString(),
                    Router.getURI(TypedController::withChar, '7', 'y').toString(),
                    // Boxed types
                    Router.getURI(TypedController::withBoxedInt, 42, 99).toString(),
                    Router.getURI(TypedController::withBoxedLong, 123456789L, 7L).toString(),
                    Router.getURI(TypedController::withBoxedBoolean, true, false).toString(),
                    Router.getURI(TypedController::withBoxedDouble, 3.14, 1.5).toString(),
                    Router.getURI(TypedController::withBoxedFloat, 2.5f, 9.81f).toString(),
                    Router.getURI(TypedController::withBoxedShort, (short) 100, (short) 5).toString(),
                    Router.getURI(TypedController::withBoxedByte, (byte) 7, (byte) 3).toString(),
                    Router.getURI(TypedController::withBoxedChar, 'x', 'y').toString(),
                    // Reference types
                    Router.getURI(TypedController::withUuid,
                            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                            UUID.fromString("660e8400-e29b-41d4-a716-446655440000")).toString(),
                    Router.getURI(TypedController::withString, "hello", "world").toString(),
                    Router.getURI(TypedController::withBigDecimal,
                            new BigDecimal("99.99"), new BigDecimal("123.45")).toString(),
                    Router.getURI(TypedController::withBigInteger,
                            new BigInteger("999999999999"), new BigInteger("123")).toString(),
                    Router.getURI(TypedController::withLocalDate,
                            LocalDate.of(2025, 6, 15), LocalDate.of(2025, 7, 20)).toString(),
                    Router.getURI(TypedController::withLocalDateTime,
                            LocalDateTime.of(2025, 6, 15, 10, 30),
                            LocalDateTime.of(2025, 7, 20, 14, 0)).toString(),
                    Router.getURI(TypedController::withLocalTime,
                            LocalTime.of(10, 30), LocalTime.of(14, 0)).toString(),
                    Router.getURI(TypedController::withInstant,
                            Instant.parse("2025-06-15T10:30:00Z"),
                            Instant.parse("2025-07-20T14:00:00Z")).toString(),
                    Router.getURI(TypedController::withEnum,
                            MyStatus.ACTIVE, MyStatus.INACTIVE).toString(),
                    // URL-encoded values
                    Router.getURI(TypedController::withString, "hello world", "foo&bar=baz").toString(),
                    Router.getURI(TypedController::withUri,
                            URI.create("https://example.com/path?foo=bar"),
                            URI.create("https://other.com?baz=1&x=2")).toString(),
                    // Optional query param: present and absent
                    Router.getURI(TypedController::withOptionalQuery, 42, Optional.of(99)).toString(),
                    Router.getURI(TypedController::withOptionalQuery, 42, Optional.empty()).toString());
        }

        @Path("/router-absolute-uri")
        public String routerAbsoluteUri() {
            return Router.getAbsoluteURI(TypedController::withInt, 42, 99).toString();
        }
    }
}
