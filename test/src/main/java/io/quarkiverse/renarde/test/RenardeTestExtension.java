package io.quarkiverse.renarde.test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.test.ExclusivityChecker;
import io.quarkus.test.junit.QuarkusTestExtension;

/*
 * We want BeforeAllCallback, but the TCCL isn't properly set up at this point
 * This works for QuarkusUnitTest.
 */
public class RenardeTestExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Store store = context.getStore(Namespace.GLOBAL);
        Class<?> testType = store.get(ExclusivityChecker.IO_QUARKUS_TESTING_TYPE, Class.class);
        // We need the QuarkusClassLoader, which is not the current class' CL
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (testType == null || testType == QuarkusTestExtension.class) {
            // Good chance it's a QuarkusTest
            Field field = QuarkusTestExtension.class.getDeclaredField("runningQuarkusApplication");
            field.setAccessible(true);
            RunningQuarkusApplication quarkusApplication = (RunningQuarkusApplication) field.get(null);
            if (quarkusApplication != null) {
                cl = quarkusApplication.getClassLoader();
            }
        }
        // For QuarkusUnitTest, the TCCL is set properly
        // For DevModeTest, no luck so far
        Class<?> csrfFilterClass = cl.loadClass(CSRFFilter.class.getName());
        if (disabledCSRF(context.getTestMethod())
                || disabledCSRF(context.getTestClass())) {
            csrfFilterClass.getDeclaredMethod("deinstall").invoke(null);
        } else {
            csrfFilterClass.getDeclaredMethod("install").invoke(null);
        }
    }

    private boolean disabledCSRF(Optional<? extends AnnotatedElement> testElement) {
        if (testElement.isEmpty()) {
            return false;
        }
        return testElement.get().isAnnotationPresent(DisableCSRFFilter.class);
    }

}
