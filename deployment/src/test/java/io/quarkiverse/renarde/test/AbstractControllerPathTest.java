package io.quarkiverse.renarde.test;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
 * plus the other hierarchies covered by the rules of the "Controller inheritance" section of the documentation.
 *
 * Every endpoint is checked both for its URI and its route.
 */
public class AbstractControllerPathTest {

    /**
     * URI expression, path of both its URI and its route, response body
     */
    private static final String[][] ENDPOINTS = {
            // Cases 1-8: no override (method inherited as-is)
            { "C1.items()", "/C1/items", "c1" }, // 1: no @Path anywhere
            { "C2.items()", "/api2/items", "c2" }, // 2: abstract @Path, no concrete @Path -> inherit
            { "C3.items()", "/C3/m3", "c3" }, // 3: method @Path only
            { "C4.items()", "/api4/m4", "c4" }, // 4: abstract @Path + method @Path
            { "C5.items()", "/app5/items", "c5" }, // 5: concrete @Path only
            { "C6.items()", "/app6/items", "c6" }, // 6: both @Path, concrete wins
            { "C7.items()", "/app7/m7", "c7" }, // 7: concrete @Path + method @Path
            { "C8.items()", "/app8/m8", "c8" }, // 8: all @Paths, concrete wins
            // Cases 9-16: override without @Path (inherits parent's method @Path if any)
            { "C9.items()", "/C9/items", "c9" }, // 9: no @Path anywhere
            { "C10.items()", "/api10/items", "c10" }, // 10: abstract @Path -> inherit
            { "C11.items()", "/C11/m11", "c11" }, // 11: inherit method @Path from parent
            { "C12.items()", "/api12/m12", "c12" }, // 12: inherit abstract @Path + method @Path
            { "C13.items()", "/app13/items", "c13" }, // 13: concrete @Path
            { "C14.items()", "/app14/items", "c14" }, // 14: concrete @Path wins over abstract
            { "C15.items()", "/app15/m15", "c15" }, // 15: concrete @Path + inherit method @Path
            { "C16.items()", "/app16/m16", "c16" }, // 16: concrete @Path wins + inherit method @Path
            // Cases 17-24: override with @Path (replaces parent's method @Path)
            { "C17.items()", "/C17/o17", "c17" }, // 17: override @Path
            { "C18.items()", "/api18/o18", "c18" }, // 18: abstract @Path + override @Path
            { "C19.items()", "/C19/o19", "c19" }, // 19: override @Path replaces parent method @Path
            { "C20.items()", "/api20/o20", "c20" }, // 20: abstract @Path + override replaces method @Path
            { "C21.items()", "/app21/o21", "c21" }, // 21: concrete @Path + override @Path
            { "C22.items()", "/app22/o22", "c22" }, // 22: concrete @Path wins + override @Path
            { "C23.items()", "/app23/o23", "c23" }, // 23: concrete @Path + override replaces method @Path
            { "C24.items()", "/app24/o24", "c24" }, // 24: concrete @Path wins + override replaces method @Path
            // Case 25: multi-level inheritance
            { "Multi.l1()", "/Multi/l1", "L1" },
            { "Multi.l2()", "/Multi/l2", "L2" },
            // Cases 26-27: baselines (no inheritance)
            { "Plain.action()", "/Plain/action", "plain" },
            { "PathPlain.action()", "/p/action", "pathplain" },
            // Case 28: absolute method path in a controller inheriting endpoints is relative to its class path
            { "Abs28.foo()", "/Abs28/foo28", "c28" },
            { "Abs28.items()", "/Abs28/items", "h28" },
            // Case 29: abstract -> concrete -> concrete
            { "Mid29.mid()", "/Mid29/mid", "mid29" },
            { "Leaf29.mid()", "/Leaf29/mid", "mid29" },
            { "Leaf29.items()", "/Leaf29/items", "b29" },
            // Case 30: abstract -> concrete with @Path -> concrete
            { "PMid30.t()", "/m30/t", "t30" },
            { "PMid30.x()", "/m30/x", "x30" },
            { "PMid30.y()", "/m30/y30", "pmid-y30" },
            { "PLeaf30.t()", "/PLeaf30/t", "t30" },
            { "PLeaf30.x()", "/PLeaf30/x", "x30" },
            { "PLeaf30.y()", "/PLeaf30/y30", "pleaf-y30" },
            // Case 31: overriding an absolute method @Path of a plain concrete controller doesn't inherit it
            { "Mid31.x()", "/abs31", "mid31" },
            { "Leaf31.x()", "/Leaf31/x", "leaf31" },
            // Case 32: controllers sharing an inherited @Path, without inherited endpoints
            { "SA32.a()", "/api32/a", "a32" },
            { "SB32.b()", "/api32/b", "b32" },
            // Case 33: the nearest parent @Path is concrete, so the abstract @Path above it is not inherited
            { "AMid33.m()", "/m33/m", "m33" },
            { "AMid33.t()", "/m33/t", "t33" },
            { "ALeaf33.m()", "/ALeaf33/m", "m33" },
            { "ALeaf33.t()", "/ALeaf33/t", "t33" },
            // Case 34: inherited path parameters, appended to the path or declared in the method @Path
            { "C34.show('5')", "/x34/show/5", "show5" },
            { "C34.edit('5')", "/x34/5/edit", "edit5" },
    };

    private static final String[] NOT_FOUND = {
            // Abstract class names must never be routes
            "/Base1/items", "/Base2/items", "/Base3/items", "/Base4/items",
            "/Base5/items", "/Base6/items", "/Base7/items", "/Base8/items",
            "/Base9/items", "/Base10/items", "/Base11/items", "/Base12/items",
            "/Base13/items", "/Base14/items", "/Base15/items", "/Base16/items",
            "/Base17/items", "/Base18/items", "/Base19/items", "/Base20/items",
            "/Base21/items", "/Base22/items", "/Base23/items", "/Base24/items",
            "/Level1/l1", "/Level2/l2", "/Level2/l1", "/Helper28/items", "/Base29/items", "/Top30/t",
            // Abstract @Path values must NOT produce routes when concrete has own @Path (concrete wins)
            "/api6/items", "/api8/m8",
            "/api14/items", "/api16/m16",
            "/api22/items", "/api24/m24",
            "/api33/m", "/api33/t",
            // Concrete class name must NOT be a fallback when endpoint uses inherited @Path
            "/C2/items", "/C4/m4",
            "/C10/items", "/C12/m12",
            "/C18/o18", "/C20/o20",
            // When an override changes the method path, the old path must not exist
            "/C11/items", "/api12/items", "/app15/items", "/app16/items",
            "/C17/items", "/api18/items", "/C19/items", "/C19/m19", "/api20/items", "/api20/m20",
            "/app21/items", "/app22/items", "/app23/items", "/app23/m23", "/app24/items", "/app24/m24",
            "/PLeaf30/y",
            // No routes leak to the root
            "/items", "/m3", "/m4", "/m7", "/m8",
            "/m11", "/m12", "/m15", "/m16",
            "/o17", "/o18", "/o19", "/o20", "/o21", "/o22", "/o23", "/o24",
            "/l1", "/l2", "/action", "/foo28", "/x", "/y30",
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AbstractControllerPathTest.class.getDeclaredClasses())
                    .addAsResource(new StringAsset(Arrays.stream(ENDPOINTS)
                            .map(endpoint -> "{uri:" + endpoint[0] + "}")
                            .collect(Collectors.joining("|"))),
                            "templates/C1/uris.txt")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testRoutes() {
        for (String[] endpoint : ENDPOINTS) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(endpoint[1])
                    .then().statusCode(200)
                    .body(Matchers.is(endpoint[2]));
        }
    }

    @Test
    public void testNotFound() {
        for (String path : NOT_FOUND) {
            RestAssured.given().urlEncodingEnabled(false)
                    .when().get(path)
                    .then().statusCode(404);
        }
    }

    @Test
    public void testUris() {
        String expected = Arrays.stream(ENDPOINTS)
                .map(endpoint -> endpoint[1])
                .collect(Collectors.joining("|"));
        RestAssured.given().urlEncodingEnabled(false)
                .when().get("/C1/uris")
                .then().statusCode(200)
                .body(Matchers.is(expected));
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

    // =====================================================================
    // Case 28: absolute method path, in a controller which inherits endpoints
    // =====================================================================

    public static abstract class Helper28 extends Controller {
        public String items() {
            return "h28";
        }
    }

    public static class Abs28 extends Helper28 {
        @Path("/foo28")
        public String foo() {
            return "c28";
        }
    }

    // =====================================================================
    // Case 29: abstract -> concrete -> concrete
    // =====================================================================

    public static abstract class Base29 extends Controller {
        public String items() {
            return "b29";
        }
    }

    public static class Mid29 extends Base29 {
        public String mid() {
            return "mid29";
        }
    }

    public static class Leaf29 extends Mid29 {
    }

    // =====================================================================
    // Case 30: abstract -> concrete with @Path -> concrete, overriding a method @Path without @Path
    // =====================================================================

    public static abstract class Top30 extends Controller {
        public String t() {
            return "t30";
        }
    }

    @Path("/m30")
    public static class PMid30 extends Top30 {
        public String x() {
            return "x30";
        }

        @Path("y30")
        public String y() {
            return "pmid-y30";
        }
    }

    public static class PLeaf30 extends PMid30 {
        @Override
        public String y() {
            return "pleaf-y30";
        }
    }

    // =====================================================================
    // Case 31: plain concrete controller with an absolute method @Path, overridden without @Path
    // =====================================================================

    public static class Mid31 extends Controller {
        @Path("/abs31")
        public String x() {
            return "mid31";
        }
    }

    public static class Leaf31 extends Mid31 {
        @Override
        public String x() {
            return "leaf31";
        }
    }

    // =====================================================================
    // Case 32: two controllers sharing an inherited @Path, without inherited endpoints
    // =====================================================================

    @Path("/api32")
    public static abstract class Base32 extends Controller {
    }

    public static class SA32 extends Base32 {
        public String a() {
            return "a32";
        }
    }

    public static class SB32 extends Base32 {
        public String b() {
            return "b32";
        }
    }

    // =====================================================================
    // Case 33: abstract with @Path -> concrete with @Path -> concrete
    // =====================================================================

    @Path("/api33")
    public static abstract class Top33 extends Controller {
        public String t() {
            return "t33";
        }
    }

    @Path("/m33")
    public static class AMid33 extends Top33 {
        public String m() {
            return "m33";
        }
    }

    public static class ALeaf33 extends AMid33 {
    }

    // =====================================================================
    // Case 34: inherited path parameters
    // =====================================================================

    public static abstract class Base34 extends Controller {
        public String show(@RestPath String id) {
            return "show" + id;
        }

        @Path("{id}/edit")
        public String edit(@RestPath String id) {
            return "edit" + id;
        }
    }

    @Path("/x34")
    public static class C34 extends Base34 {
    }
}
