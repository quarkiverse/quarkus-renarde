package io.quarkiverse.renarde.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "renarde", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class RenardeConfig {

    /**
     * Renarde Auth config
     */
    @ConfigItem
    public RenardeAuthConfig auth;

}
