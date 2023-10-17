package io.quarkiverse.renarde.util;

import io.quarkus.arc.Arc;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.qute.TemplateGlobal;

@TemplateGlobal
public class Globals {
    /**
     * Variant of inject:csrf that can actually be null if CSRF is disabled via configuration. Otherwise Qute
     * will validate that it's here and throw at compile time if CSRF is disabled.
     * FIXME: use https://github.com/quarkusio/quarkus/pull/36524 once released
     */
    public static CsrfTokenParameterProvider csrf() {
        return Arc.container().instance(CsrfTokenParameterProvider.class).get();
    }
}
