package io.quarkiverse.renarde.test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.csrf.reactive.runtime.CsrfTokenUtils;

// We can't use Arc to get the config object, and we can't rely on config defaults which don't work in native tests
public class CSRF {
    // We can use SecureRandom here, because this is a test module and we don't compile tests natively
    private final static SecureRandom secureRandom = new SecureRandom();

    public static String makeCSRFToken() {
        Optional<String> tokenSignatureKey = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.csrf-reactive.token-signature-key", String.class);
        Optional<Integer> tokenSize = ConfigProvider.getConfig().getOptionalValue("quarkus.csrf-reactive.token-size",
                Integer.class);
        byte[] tokenBytes = new byte[tokenSize.orElse(16)];
        secureRandom.nextBytes(tokenBytes);
        if (tokenSignatureKey.isPresent())
            return CsrfTokenUtils.signCsrfToken(tokenBytes, tokenSignatureKey.get());
        else
            return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public static String getTokenCookieName() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.csrf-reactive.cookie-name", String.class)
                .orElse("csrf-token");
    }

    public static String getTokenFormName() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.csrf-reactive.form-field-name", String.class)
                .orElse("csrf-token");
    }
}
