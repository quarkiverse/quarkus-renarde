package io.quarkiverse.renarde.router;

@FunctionalInterface
public interface Method2<Target, P1, P2> {
    Object method(Target target, P1 param1, P2 param2);
}
