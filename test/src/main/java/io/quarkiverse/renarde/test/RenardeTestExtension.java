package io.quarkiverse.renarde.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import io.quarkus.test.ExclusivityChecker;

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
        // For QuarkusUnitTest, the TCCL is set properly
        // For DevModeTest, no luck so far
        Class<?> csrfFilterClass = cl.loadClass(CSRFFilter.class.getName());
        // make sure we use the proper annotation class for the lookup (using the QCL)
        Class<? extends Annotation> disabledCSRFFilterClass = (Class<? extends Annotation>) cl
                .loadClass(DisableCSRFFilter.class.getName());
        if (disabledCSRF(context.getTestMethod(), disabledCSRFFilterClass)
                || disabledCSRF(context.getTestClass(), disabledCSRFFilterClass)) {
            csrfFilterClass.getDeclaredMethod("deinstall").invoke(null);
        } else {
            csrfFilterClass.getDeclaredMethod("install").invoke(null);
        }
    }

    private boolean disabledCSRF(Optional<? extends AnnotatedElement> testElement,
            Class<? extends Annotation> disabledCSRFFilterClass) {
        if (testElement.isEmpty()) {
            return false;
        }
        return testElement.get().isAnnotationPresent(disabledCSRFFilterClass);
    }

}
