package io.quarkiverse.renarde.router;

import java.net.URI;

@FunctionalInterface
public interface RouterMethod {
    URI getRoute(boolean absolute, Object... args);
}
