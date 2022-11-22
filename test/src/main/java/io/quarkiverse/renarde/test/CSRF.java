package io.quarkiverse.renarde.test;

import java.security.SecureRandom;
import java.util.Base64;

import io.quarkus.arc.Arc;
import io.quarkus.csrf.reactive.runtime.CsrfReactiveConfig;
import io.quarkus.csrf.reactive.runtime.CsrfTokenUtils;

public class CSRF {
    // We can use SecureRandom here, because this is a test module and we don't compile tests natively
    private final static SecureRandom secureRandom = new SecureRandom();

    public static String makeCSRFToken() {
        CsrfReactiveConfig config = Arc.container().instance(CsrfReactiveConfig.class).get();
        byte[] tokenBytes = new byte[config.tokenSize];
        secureRandom.nextBytes(tokenBytes);
        if (config.tokenSignatureKey.isPresent())
            return CsrfTokenUtils.signCsrfToken(tokenBytes, config.tokenSignatureKey.get());
        else
            return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public static String getTokenCookieName() {
        CsrfReactiveConfig config = Arc.container().instance(CsrfReactiveConfig.class).get();
        return config.cookieName;
    }

    public static String getTokenFormName() {
        CsrfReactiveConfig config = Arc.container().instance(CsrfReactiveConfig.class).get();
        return config.formFieldName;
    }
}
