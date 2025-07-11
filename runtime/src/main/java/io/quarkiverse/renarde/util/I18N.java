package io.quarkiverse.renarde.util;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Cookie;

import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkiverse.renarde.impl.RenardeConfigBean;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@Named("i18n")
@RequestScoped
public class I18N {

    public final static String LOCALE_COOKIE_NAME = "_renarde_locale";
    /**
     * @deprecated use {@link I18N#LOCALE_COOKIE_NAME}
     */
    @Deprecated
    public final static String LANGUAGE_COOKIE_NAME = LOCALE_COOKIE_NAME;

    @Inject
    HttpServerRequest request;

    @Inject
    LocalesBuildTimeConfig localesConfig;

    @Inject
    RenardeConfigBean renardeConfig;

    private Locale locale = null;
    private boolean localeOverridden = false;

    public void set(String language) {
        setForCurrentRequest(language);
        localeOverridden = true;
    }

    public void setForCurrentRequest(String language) {
        Objects.requireNonNull(language);
        // check that we support it
        Locale found = findSupportedLocale(language);
        if (found == null) {
            throw new IllegalArgumentException(
                    "Language " + language + " not supported, please add it to the 'quarkus.locales' configuration");
        }
        this.locale = found;
    }

    private Locale findSupportedLocale(String language) {
        // FIXME: simplistic
        for (Locale locale : localesConfig.locales()) {
            if (locale.getLanguage().equals(language)) {
                return locale;
            }
        }
        return null;
    }

    /**
     * Returns the language part of the current locale
     *
     * @return the language part of the current locale
     * @deprecated use {@link #getLanguage()}
     */
    @Deprecated
    public String get() {
        return getLanguage();
    }

    /**
     * Returns the language part of the current locale
     *
     * @return the language part of the current locale
     */
    public String getLanguage() {
        return getLocale().getLanguage();
    }

    /**
     * The current locale, as obtained via a cookie override, or HTTP headers, or the default.
     *
     * @return the current locale, as obtained via a cookie override, or HTTP headers, or the default.
     */
    public Locale getLocale() {
        return locale != null ? locale : localesConfig.defaultLocale().orElse(Locale.getDefault());
    }

    void readLanguageCookie(ResteasyReactiveContainerRequestContext requestContext) {
        // first try from our cookie
        Cookie cookie = requestContext.getCookies().get(LOCALE_COOKIE_NAME);
        if (cookie != null) {
            // use the cookie if it's valid
            locale = findSupportedLocale(cookie.getValue());
            if (locale != null) {
                return;
            }
            // invalid cookie locale, fallback to use headers
        }
        // if not, use the accept header
        List<Locale> acceptableLanguages = HeaderUtil.getAcceptableLanguages(requestContext.getHeaders());
        for (Locale acceptableLanguage : acceptableLanguages) {
            if (acceptableLanguage.getLanguage().equals("*")) {
                // go with default language
                break;
            }
            // do we support it?
            // FIXME: perhaps only look at the primary language?
            if (localesConfig.locales().contains(acceptableLanguage)) {
                locale = acceptableLanguage;
                return;
            }
        }
        // we didn't find any match between our locales and the user's so → default value
        locale = localesConfig.defaultLocale().orElse(Locale.getDefault());
    }

    void setLanguageCookie() {
        HttpServerResponse response = request.response();
        if (localeOverridden && !response.headWritten()) {
            response.addCookie(
                    io.vertx.core.http.Cookie.cookie(LOCALE_COOKIE_NAME, locale.toString())
                            .setPath("/")
                            .setSameSite(CookieSameSite.LAX)
                            .setSecure(request.isSSL()));
        }
    }

    public String getMessage(String key) {
        String message = message(key);
        return message != null ? message : key;
    }

    public String formatMessage(String key, Object... params) {
        String message = message(key);
        return message != null ? String.format(message, params) : key;
    }

    private String message(String key) {
        return renardeConfig.getMessage(getLanguage(), key);
    }
}
