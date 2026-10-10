package io.quarkiverse.renarde.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.LoginPage;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A @LoginPage inherited by several controllers has no single URI, so the build must fail.
 */
public class AmbiguousLoginPageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CustomLoginControllerTest.MyUser.class, CustomLoginControllerTest.MyUserProvider.class,
                            BaseLoginController.class, LoginControllerA.class, LoginControllerB.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .assertException(BuildFailures.rootCauseMessageContains("Multiple @LoginPage methods", "LoginControllerA.login",
                    "LoginControllerB.login"));

    @Test
    public void test() {
        Assertions.fail("Build should have failed");
    }

    public static abstract class BaseLoginController extends Controller {
        @LoginPage
        public String login() {
            return "login page";
        }
    }

    public static class LoginControllerA extends BaseLoginController {
    }

    public static class LoginControllerB extends BaseLoginController {
    }
}
