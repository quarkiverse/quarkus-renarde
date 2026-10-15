package io.quarkiverse.renarde.test;

import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

final class BuildFailures {

    private BuildFailures() {
    }

    /**
     * Asserts that the root cause message of a build failure contains all the given parts.
     */
    static Consumer<Throwable> rootCauseMessageContains(String... parts) {
        return t -> {
            Throwable cause = t;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            for (String part : parts) {
                Assertions.assertTrue(cause.getMessage().contains(part), cause.getMessage());
            }
        };
    }
}
