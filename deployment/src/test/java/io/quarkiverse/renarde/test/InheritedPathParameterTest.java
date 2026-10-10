package io.quarkiverse.renarde.test;

import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.test.QuarkusUnitTest;

/**
 * The route of an inherited method is shared by all subclasses, so a path parameter of the method can't be declared by the
 * class path of only some of them: the build must fail.
 */
public class InheritedPathParameterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BaseController.class, SubController.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .assertException(BuildFailures.rootCauseMessageContains("SubController", "BaseController.show",
                    "path parameter 'id'"));

    @Test
    public void test() {
        Assertions.fail("Build should have failed");
    }

    public static abstract class BaseController extends Controller {
        public String show(@RestPath String id) {
            return id;
        }
    }

    @Path("/x/{id}")
    public static class SubController extends BaseController {
    }
}
