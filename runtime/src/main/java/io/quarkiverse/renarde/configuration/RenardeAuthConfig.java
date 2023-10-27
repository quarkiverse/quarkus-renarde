package io.quarkiverse.renarde.configuration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RenardeAuthConfig {

    /**
     * Option to control the name of the cookie used to redirect the user back
     * to where he wants to get access to.
     */
    @ConfigItem(defaultValue = "quarkus-redirect-location")
    public String locationCookie;

}
