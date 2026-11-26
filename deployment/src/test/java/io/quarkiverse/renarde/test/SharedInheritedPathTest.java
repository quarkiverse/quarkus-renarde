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
 * Two controllers inheriting the same endpoints under the same inherited @Path would collide, so the build must fail.
 */
public class SharedInheritedPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BaseController.class, ControllerA.class, ControllerB.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .assertException(BuildFailures.rootCauseMessageContains("ControllerA", "ControllerB", "BaseController", "/api"));

    @Test
    public void test() {
        Assertions.fail("Build should have failed");
    }

    @Path("/api")
    public static abstract class BaseController extends Controller {
        public String items() {
            return "items";
        }
    }

    public static class ControllerA extends BaseController {
    }

    public static class ControllerB extends BaseController {
    }
}
