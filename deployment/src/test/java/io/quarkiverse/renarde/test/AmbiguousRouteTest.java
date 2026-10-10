package io.quarkiverse.renarde.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that overloaded methods with the same URI param count
 * cause a build-time failure with a clear error message.
 */
// TODO: de mporw na ta valw se ena test mono
public class AmbiguousRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BadController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .assertException(t -> {
                String msg = findRootMessage(t);
                assertTrue(msg.contains("Ambiguous route"),
                        "Expected ambiguous route error but got: " + msg);
                assertTrue(msg.contains("BadController.find"),
                        "Expected error to reference BadController.find but got: " + msg);
            });

    private static String findRootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    @Test
    public void testBuildFailsForAmbiguousOverloads() {
        // The build should fail before this test runs.
        // The assertException above validates the error.
    }

    public static class BadController extends Controller {

        // Both have 1 URI param — ambiguous
        public String find(@RestPath Long id) {
            return "by-id:" + id;
        }

        public String find(@RestQuery String name) {
            return "by-name:" + name;
        }
    }
}