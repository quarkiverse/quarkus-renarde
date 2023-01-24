package io.quarkiverse.renarde.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class ExcludedControllerBuildItem extends MultiBuildItem {
    public final DotName excludedClass;

    public ExcludedControllerBuildItem(DotName excludedClass) {
        this.excludedClass = excludedClass;
    }
}
