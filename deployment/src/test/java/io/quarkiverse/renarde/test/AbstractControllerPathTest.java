package io.quarkiverse.renarde.test;

import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Comprehensive test for controller inheritance — single source of truth.
 *
 * Full combinatorial matrix of:
 * - Abstract class @Path: none vs present
 * - Abstract method @Path: none vs present
 * - Concrete class @Path: none vs present
 * - Override: none, without @Path, with @Path
 *
 * Rules:
 * 1. Inherit parent @Path: if abstract has @Path and concrete doesn't, concrete inherits it
 * 2. Concrete wins: if both have @Path, concrete's takes precedence
 * 3. Inherit method @Path on override: override without @Path inherits parent method's @Path
 * 4. Override @Path replaces: override with @Path replaces parent method's @Path
 *
 * 27 total cases (24 inheritance + 3 additional).
 */
public class AbstractControllerPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            // Cases 1-8: no override
                            Base1.class, C1.class,
                            Base2.class, C2.class,
                            Base3.class, C3.class,
                            Base4.class, C4.class,
                            Base5.class, C5.class,
                            Base6.class, C6.class,
                            Base7.class, C7.class,
                            Base8.class, C8.class,
                            // Cases 9-16: override without @Path
                            Base9.class, C9.class,
                            Base10.class, C10.class,
                            Base11.class, C11.class,
                            Base12.class, C12.class,
                            Base13.class, C13.class,
                            Base14.class, C14.class,
                            Base15.class, C15.class,
                            Base16.class, C16.class,
                            // Cases 17-24: override with @Path
                            Base17.class, C17.class,
                            Base18.class, C18.class,
                            Base19.class, C19.class,
                            Base20.class, C20.class,
                            Base21.class, C21.class,
                            Base22.class, C22.class,
                            Base23.class, C23.class,
                            Base24.class, C24.class,
                            // Cases 25-27
                            Level1.class, Level2.class, Multi.class,
                            Plain.class, PathPlain.class)
                    .addAsResource(new StringAsset(
                            "{uri:C1.items()}|{uri:C2.items()}|{uri:C3.items()}|{uri:C4.items()}"
                                    + "|{uri:C5.items()}|{uri:C6.items()}|{uri:C7.items()}|{uri:C8.items()}"
                                    + "|{uri:C9.items()}|{uri:C10.items()}|{uri:C11.items()}|{uri:C12.items()}"
                                    + "|{uri:C13.items()}|{uri:C14.items()}|{uri:C15.items()}|{uri:C16.items()}"
                                    + "|{uri:C17.items()}|{uri:C18.items()}|{uri:C19.items()}|{uri:C20.items()}"
                                    + "|{uri:C21.items()}|{uri:C22.items()}|{uri:C23.items()}|{uri:C24.items()}"
                                    + "|{uri:Multi.l1()}|{uri:Multi.l2()}"
                                    + "|{uri:Plain.action()}|{uri:PathPlain.action()}"),
                            "templates/C1/uris.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    // =====================================================================
    // Test methods
    // =====================================================================

    @Test
    public void testConcreteEndpoints() {
        String[][] cases = {
                // Cases 1-8: no override (method inherited as-is)
                { "/C1/items", "c1" }, // 1: no @Path anywhere
                { "/api2/items", "c2" }, // 2: abstract @Path, no concrete @Path -> inherit
                { "/C3/m3", "c3" }, // 3: method @Path only
                { "/api4/m4", "c4" }, // 4: abstract @Path + method @Path
                { "/app5/items", "c5" }, // 5: concrete @Path only
                { "/app6/items", "c6" }, // 6: both @Path, concrete wins
                { "/app7/m7", "c7" }, // 7: concrete @Path + method @Path
                { "/app8/m8", "c8" }, // 8: all @Paths, concrete wins
                // Cases 9-16: override without @Path (inherits parent's method @Path if any)
                { "/C9/items", "c9" }, // 9: no @Path anywhere
                { "/api10/items", "c10" }, // 10: abstract @Path -> inherit
                { "/C11/m11", "c11" }, // 11: inherit method @Path from parent
                { "/api12/m12", "c12" }, // 12: inherit abstract @Path + method @Path
                { "/app13/items", "c13" }, // 13: concrete @Path
                { "/app14/items", "c14" }, // 14: concrete @Path wins over abstract
                { "/app15/m15", "c15" }, // 15: concrete @Path + inherit method @Path
                { "/app16/m16", "c16" }, // 16: concrete @Path wins + inherit method @Path
                // Cases 17-24: override with @Path (replaces parent's method @Path)
                { "/C17/o17", "c17" }, // 17: override @Path
                { "/api18/o18", "c18" }, // 18: abstract @Path + override @Path
                { "/C19/o19", "c19" }, // 19: override @Path replaces parent method @Path
                { "/api20/o20", "c20" }, // 20: abstract @Path + override replaces method @Path
                { "/app21/o21", "c21" }, // 21: concrete @Path + override @Path
                { "/app22/o22", "c22" }, // 22: concrete @Path wins + override @Path
                { "/app23/o23", "c23" }, // 23: concrete @Path + override replaces method @Path
                { "/app24/o24", "c24" }, // 24: concrete @Path wins + override replaces method @Path
                // Case 25: multi-level inheritance
                { "/Multi/l1", "L1" },
                { "/Multi/l2", "L2" },
                // Cases 26-27: baselines (no inheritance)
                { "/Plain/action", "plain" },
                { "/p/action", "pathplain" },
        };
        for (String[] c : cases) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(c[0])
                    .then().statusCode(200)
                    .body(Matchers.is(c[1]));
        }
    }

    @Test
    public void testAbstractEndpointsNotAccessible() {
        String[] paths = {
                // Abstract class names must never be routes
                "/Base1/items", "/Base2/items", "/Base3/items", "/Base4/items",
                "/Base5/items", "/Base6/items", "/Base7/items", "/Base8/items",
                "/Base9/items", "/Base10/items", "/Base11/items", "/Base12/items",
                "/Base13/items", "/Base14/items", "/Base15/items", "/Base16/items",
                "/Base17/items", "/Base18/items", "/Base19/items", "/Base20/items",
                "/Base21/items", "/Base22/items", "/Base23/items", "/Base24/items",
                "/Level1/l1", "/Level2/l2", "/Level2/l1",
                // Abstract @Path values must NOT produce routes when concrete has own @Path (concrete wins)
                "/api6/items", "/api8/m8",
                "/api14/items", "/api16/m16",
                "/api22/items", "/api24/m24",
                // Concrete class name must NOT be a fallback when endpoint uses inherited @Path
                "/C2/items", "/C4/m4",
                "/C10/items", "/C12/m12",
                "/C18/o18", "/C20/o20",
        };
        for (String path : paths) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(path)
                    .then().statusCode(404);
        }
    }

    @Test
    public void testNoLeakedRootRoutes() {
        String[] paths = {
                "/items", "/m3", "/m4", "/m7", "/m8",
                "/m11", "/m12", "/m15", "/m16",
                "/o17", "/o18", "/o19", "/o20", "/o21", "/o22", "/o23", "/o24",
                "/l1", "/l2", "/action",
        };
        for (String path : paths) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(path)
                    .then().statusCode(404);
        }
    }

    @Test
    public void testUriGeneration() {
        String body = RestAssured.given().urlEncodingEnabled(false)
                .when().get("/C1/uris")
                .then().statusCode(200)
                .extract().body().asString();
        String expected = String.join("|",
                "/C1/items", // 1
                "/api2/items", // 2
                "/C3/m3", // 3
                "/api4/m4", // 4
                "/app5/items", // 5
                "/app6/items", // 6
                "/app7/m7", // 7
                "/app8/m8", // 8
                "/C9/items", // 9
                "/api10/items", // 10
                "/C11/m11", // 11
                "/api12/m12", // 12
                "/app13/items", // 13
                "/app14/items", // 14
                "/app15/m15", // 15
                "/app16/m16", // 16
                "/C17/o17", // 17
                "/api18/o18", // 18
                "/C19/o19", // 19
                "/api20/o20", // 20
                "/app21/o21", // 21
                "/app22/o22", // 22
                "/app23/o23", // 23
                "/app24/o24", // 24
                "/Multi/l1", // 25a
                "/Multi/l2", // 25b
                "/Plain/action", // 26
                "/p/action"); // 27
        Assertions.assertEquals(expected, body);
    }

    @Test
    public void testOverriddenMethodsNotDuplicated() {
        // When override changes the method path, the old path must not exist
        String[] paths = {
                // Cases 11, 12: override inherits method @Path, so method-name path must not exist
                "/C11/items",
                "/api12/items",
                // Cases 15, 16: same with concrete @Path
                "/app15/items",
                "/app16/items",
                // Cases 17-24: override has own @Path, so method-name path must not exist
                "/C17/items",
                "/api18/items",
                "/C19/items", "/C19/m19",
                "/api20/items", "/api20/m20",
                "/app21/items",
                "/app22/items",
                "/app23/items", "/app23/m23",
                "/app24/items", "/app24/m24",
        };
        for (String path : paths) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(path)
                    .then().statusCode(404);
        }
    }

    // =====================================================================
    // Cases 1-8: No override (method inherited as-is)
    // =====================================================================

    // Case 1: no @Path anywhere
    public static abstract class Base1 extends Controller {
        public String items() {
            return "c1";
        }
    }

    public static class C1 extends Base1 {
        @CheckedTemplate
        public static class Templates {
            public static native TemplateInstance uris();
        }

        @Path("uris")
        public TemplateInstance uris() {
            return Templates.uris();
        }
    }

    // Case 2: abstract @Path("/api2"), no concrete @Path -> inherit
    @Path("/api2")
    public static abstract class Base2 extends Controller {
        public String items() {
            return "c2";
        }
    }

    public static class C2 extends Base2 {
    }

    // Case 3: no abstract @Path, method @Path("m3")
    public static abstract class Base3 extends Controller {
        @Path("m3")
        public String items() {
            return "c3";
        }
    }

    public static class C3 extends Base3 {
    }

    // Case 4: abstract @Path("/api4") + method @Path("m4")
    @Path("/api4")
    public static abstract class Base4 extends Controller {
        @Path("m4")
        public String items() {
            return "c4";
        }
    }

    public static class C4 extends Base4 {
    }

    // Case 5: no abstract @Path, concrete @Path("/app5")
    public static abstract class Base5 extends Controller {
        public String items() {
            return "c5";
        }
    }

    @Path("/app5")
    public static class C5 extends Base5 {
    }

    // Case 6: abstract @Path("/api6"), concrete @Path("/app6") -> concrete wins
    @Path("/api6")
    public static abstract class Base6 extends Controller {
        public String items() {
            return "c6";
        }
    }

    @Path("/app6")
    public static class C6 extends Base6 {
    }

    // Case 7: no abstract @Path, method @Path("m7"), concrete @Path("/app7")
    public static abstract class Base7 extends Controller {
        @Path("m7")
        public String items() {
            return "c7";
        }
    }

    @Path("/app7")
    public static class C7 extends Base7 {
    }

    // Case 8: abstract @Path("/api8"), method @Path("m8"), concrete @Path("/app8") -> concrete wins
    @Path("/api8")
    public static abstract class Base8 extends Controller {
        @Path("m8")
        public String items() {
            return "c8";
        }
    }

    @Path("/app8")
    public static class C8 extends Base8 {
    }

    // =====================================================================
    // Cases 9-16: Override WITHOUT @Path (inherits parent's method @Path)
    // =====================================================================

    // Case 9: no @Path anywhere, override
    public static abstract class Base9 extends Controller {
        public String items() {
            return "b9";
        }
    }

    public static class C9 extends Base9 {
        @Override
        public String items() {
            return "c9";
        }
    }

    // Case 10: abstract @Path("/api10"), override without @Path -> inherit class @Path
    @Path("/api10")
    public static abstract class Base10 extends Controller {
        public String items() {
            return "b10";
        }
    }

    public static class C10 extends Base10 {
        @Override
        public String items() {
            return "c10";
        }
    }

    // Case 11: method @Path("m11"), override without @Path -> inherit method @Path
    public static abstract class Base11 extends Controller {
        @Path("m11")
        public String items() {
            return "b11";
        }
    }

    public static class C11 extends Base11 {
        @Override
        public String items() {
            return "c11";
        }
    }

    // Case 12: abstract @Path("/api12") + method @Path("m12"), override without @Path -> inherit both
    @Path("/api12")
    public static abstract class Base12 extends Controller {
        @Path("m12")
        public String items() {
            return "b12";
        }
    }

    public static class C12 extends Base12 {
        @Override
        public String items() {
            return "c12";
        }
    }

    // Case 13: concrete @Path("/app13"), override without @Path
    public static abstract class Base13 extends Controller {
        public String items() {
            return "b13";
        }
    }

    @Path("/app13")
    public static class C13 extends Base13 {
        @Override
        public String items() {
            return "c13";
        }
    }

    // Case 14: abstract @Path("/api14"), concrete @Path("/app14") -> concrete wins, override without @Path
    @Path("/api14")
    public static abstract class Base14 extends Controller {
        public String items() {
            return "b14";
        }
    }

    @Path("/app14")
    public static class C14 extends Base14 {
        @Override
        public String items() {
            return "c14";
        }
    }

    // Case 15: method @Path("m15"), concrete @Path("/app15"), override without @Path -> inherit method @Path
    public static abstract class Base15 extends Controller {
        @Path("m15")
        public String items() {
            return "b15";
        }
    }

    @Path("/app15")
    public static class C15 extends Base15 {
        @Override
        public String items() {
            return "c15";
        }
    }

    // Case 16: all @Paths, concrete wins, override without @Path -> inherit method @Path
    @Path("/api16")
    public static abstract class Base16 extends Controller {
        @Path("m16")
        public String items() {
            return "b16";
        }
    }

    @Path("/app16")
    public static class C16 extends Base16 {
        @Override
        public String items() {
            return "c16";
        }
    }

    // =====================================================================
    // Cases 17-24: Override WITH @Path (replaces parent's method @Path)
    // =====================================================================

    // Case 17: no @Path anywhere, override with @Path("o17")
    public static abstract class Base17 extends Controller {
        public String items() {
            return "b17";
        }
    }

    public static class C17 extends Base17 {
        @Override
        @Path("o17")
        public String items() {
            return "c17";
        }
    }

    // Case 18: abstract @Path("/api18"), override with @Path("o18")
    @Path("/api18")
    public static abstract class Base18 extends Controller {
        public String items() {
            return "b18";
        }
    }

    public static class C18 extends Base18 {
        @Override
        @Path("o18")
        public String items() {
            return "c18";
        }
    }

    // Case 19: method @Path("m19"), override with @Path("o19") -> replaces
    public static abstract class Base19 extends Controller {
        @Path("m19")
        public String items() {
            return "b19";
        }
    }

    public static class C19 extends Base19 {
        @Override
        @Path("o19")
        public String items() {
            return "c19";
        }
    }

    // Case 20: abstract @Path("/api20") + method @Path("m20"), override with @Path("o20")
    @Path("/api20")
    public static abstract class Base20 extends Controller {
        @Path("m20")
        public String items() {
            return "b20";
        }
    }

    public static class C20 extends Base20 {
        @Override
        @Path("o20")
        public String items() {
            return "c20";
        }
    }

    // Case 21: concrete @Path("/app21"), override with @Path("o21")
    public static abstract class Base21 extends Controller {
        public String items() {
            return "b21";
        }
    }

    @Path("/app21")
    public static class C21 extends Base21 {
        @Override
        @Path("o21")
        public String items() {
            return "c21";
        }
    }

    // Case 22: abstract @Path("/api22"), concrete @Path("/app22") -> concrete wins, override @Path("o22")
    @Path("/api22")
    public static abstract class Base22 extends Controller {
        public String items() {
            return "b22";
        }
    }

    @Path("/app22")
    public static class C22 extends Base22 {
        @Override
        @Path("o22")
        public String items() {
            return "c22";
        }
    }

    // Case 23: method @Path("m23"), concrete @Path("/app23"), override @Path("o23") -> replaces
    public static abstract class Base23 extends Controller {
        @Path("m23")
        public String items() {
            return "b23";
        }
    }

    @Path("/app23")
    public static class C23 extends Base23 {
        @Override
        @Path("o23")
        public String items() {
            return "c23";
        }
    }

    // Case 24: all @Paths, concrete wins, override @Path("o24") replaces method @Path
    @Path("/api24")
    public static abstract class Base24 extends Controller {
        @Path("m24")
        public String items() {
            return "b24";
        }
    }

    @Path("/app24")
    public static class C24 extends Base24 {
        @Override
        @Path("o24")
        public String items() {
            return "c24";
        }
    }

    // =====================================================================
    // Case 25: Multi-level inheritance (abstract -> abstract -> concrete)
    // =====================================================================

    public static abstract class Level1 extends Controller {
        public String l1() {
            return "L1";
        }
    }

    public static abstract class Level2 extends Level1 {
        public String l2() {
            return "L2";
        }
    }

    public static class Multi extends Level2 {
    }

    // =====================================================================
    // Cases 26-27: Baselines (no inheritance)
    // =====================================================================

    // Case 26: no inheritance, no @Path
    public static class Plain extends Controller {
        public String action() {
            return "plain";
        }
    }

    // Case 27: no inheritance, with @Path
    @Path("/p")
    public static class PathPlain extends Controller {
        public String action() {
            return "pathplain";
        }
    }
}