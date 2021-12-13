package io.quarkiverse.renarde.router;

@FunctionalInterface
public interface Method1<Target, P1> {
    Object method(Target target, P1 param1);
}
