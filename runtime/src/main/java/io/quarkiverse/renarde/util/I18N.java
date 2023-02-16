package io.quarkiverse.renarde.util;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Cookie;

import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkiverse.renarde.impl.RenardeConfig;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@Named("i18n")
@RequestScoped
public class I18N {

    public final static String LANGUAGE_COOKIE_NAME = "_renarde_language";

    @Inject
    HttpServerRequest request;

    @Inject
    LocalesBuildTimeConfig localesConfig;

    @Inject
    RenardeConfig renardeConfig;

    private String language = null;
    private boolean languageOverridden = false;

    public void set(String language) {
        Objects.requireNonNull(language);
        // check that we support it
        boolean found = false;
        for (Locale locale : localesConfig.locales) {
            if (locale.getLanguage().equals(language)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException(
                    "Language " + language + " not supported, please add it to the 'quarkus.locales' configuration");
        }
        languageOverridden = true;
        this.language = language;
    }

    public String get() {
        return language;
    }

    void readLanguageCookie(ResteasyReactiveContainerRequestContext requestContext) {
        // first try from our cookie
        Cookie cookie = requestContext.getCookies().get(LANGUAGE_COOKIE_NAME);
        if (cookie != null) {
            language = cookie.getValue();
            return;
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
            if (localesConfig.locales.contains(acceptableLanguage)) {
                language = acceptableLanguage.getLanguage();
                return;
            }
        }
        // we didn't find any match between our locales and the user's so → default value
        language = localesConfig.defaultLocale.getLanguage();
    }

    void setLanguageCookie() {
        HttpServerResponse response = request.response();
        if (languageOverridden && !response.headWritten()) {
            response.addCookie(
                    io.vertx.core.http.Cookie.cookie(LANGUAGE_COOKIE_NAME, language)
                            .setPath("/")
                            .setSameSite(CookieSameSite.LAX));
        }
    }

    public String getMessage(String key) {
        return renardeConfig.getMessage(get(), key);
    }

    public String formatMessage(String key, Object... params) {
        String message = getMessage(key);
        return message != null ? String.format(message, params) : key;
    }
}
