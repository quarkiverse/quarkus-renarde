package io.quarkiverse.renarde.router;

@FunctionalInterface
public interface Method3V<Target, P1, P2, P3> {
    void method(Target target, P1 param1, P2 param2, P3 param3);
}
