package io.quarkiverse.renarde.configuration;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.renarde")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface RenardeConfig {

    /**
     * Renarde Auth config
     */
    public RenardeAuthConfig auth();

    public interface RenardeAuthConfig {

        public interface Redirect {

            enum Type {
                cookie,
                query
            }

            /**
             * Specifies the redirect strategy to use.
             * Defaults to <code>cookie</code>.
             */
            @WithDefault("cookie")
            Type type();

            /**
             * Option to control the name of the cookie used to redirect the user back
             * to where he wants to get access to.
             */
            @WithDefault("quarkus-redirect-location")
            String cookie();

        }

        /**
         * Renarde Auth Redirect config
         */
        Redirect redirect();

        /**
         * When true, authentication cookies use {@code quarkus.http.root-path} as their path.
         * When false, cookies use {@code "/"}.
         */
        @WithDefault("true")
        boolean scopeCookiesToRootPath();

        /**
         * The duration of the JWT token expiration. Defaults to 10 days.
         * Uses ISO-8601 duration format (e.g. P30D for 30 days, PT12H for 12 hours).
         */
        @WithDefault("P10D")
        Duration tokenExpiration();

        /**
         * Please do not use and use <code>quarkus.renarde.auth.redirect.cookie</code> instead.
         *
         * @deprecated use {@link Redirect#cookie()}
         */
        @Deprecated
        @WithDefault("${quarkus.renarde.auth.redirect.cookie}")
        public String locationCookie();

    }
}
