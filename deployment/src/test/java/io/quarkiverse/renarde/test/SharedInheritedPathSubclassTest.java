package io.quarkiverse.renarde.test;

import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A controller and its subclass both serving its endpoints under the same inherited @Path would collide, so the build
 * must fail.
 */
public class SharedInheritedPathSubclassTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BaseController.class, ParentController.class, ChildController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .assertException(BuildFailures.rootCauseMessageContains("ParentController", "ChildController",
                    "both serve the endpoints of", "/api"));

    @Test
    public void test() {
        Assertions.fail("Build should have failed");
    }

    @Path("/api")
    public static abstract class BaseController extends Controller {
    }

    public static class ParentController extends BaseController {
        public String items() {
            return "items";
        }
    }

    public static class ChildController extends ParentController {
    }
}
