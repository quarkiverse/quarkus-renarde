package io.quarkiverse.renarde.router;

@FunctionalInterface
public interface Method2V<Target, P1, P2> {
    void method(Target target, P1 param1, P2 param2);
}
