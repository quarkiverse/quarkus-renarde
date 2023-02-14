package io.quarkiverse.renarde.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.core.multipart.FormData.FormValue;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@Named("flash")
@RequestScoped
public class Flash {

    @Inject
    HttpServerRequest request;

    @Inject
    Validation validation;

    private Map<String, Object> values = new HashMap<>();
    private Map<String, Object> futureValues = new HashMap<>();

    public final static String FLASH_COOKIE_NAME = "_renarde_flash";

    public void setFlashCookie() {
        setFlashCookie(request.response(), futureValues);
    }

    public static void setFlashCookie(HttpServerResponse response, Map<String, Object> values) {
        // FIXME: expiry, others?
        // in some cases with exception mappers, it appears the filters get invoked twice
        if (!response.headWritten())
            response.addCookie(
                    Cookie.cookie(FLASH_COOKIE_NAME, encodeCookieValue(values))
                            .setPath("/")
                            .setHttpOnly(true)
                            .setSameSite(CookieSameSite.LAX));
    }

    public static String encodeCookieValue(Map<String, Object> values) {
        return Base64.getEncoder().encodeToString(marshallMap(values));
    }

    public void handleFlashCookie() {
        Cookie cookie = request.getCookie(FLASH_COOKIE_NAME);
        if (cookie != null) {
            Map<String, Object> data = decodeCookieValue(cookie.getValue());
            if (data != null) {
                values.putAll(data);
                validation.loadErrorsFromFlash();
            }
        }
        // must do this after we've read the value, otherwise we can't read it, for some reason
        request.response().removeCookie(FLASH_COOKIE_NAME);
    }

    public static Map<String, Object> decodeCookieValue(String value) {
        if (value != null && !value.isEmpty()) {
            byte[] bytes = value.getBytes();
            if (bytes != null && bytes.length != 0) {
                byte[] decoded = Base64.getDecoder().decode(bytes);
                // API says it can't be null
                if (decoded.length > 0) {
                    return unmarshallMap(decoded);
                }
            }
        }
        return null;
    }

    private static byte[] marshallMap(Map<String, Object> data) {
        String json = Json.encode(data);
        // FIXME: this is optimistic
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> unmarshallMap(byte[] data) {
        String json = new String(data, StandardCharsets.UTF_8);
        JsonObject obj = (JsonObject) Json.decodeValue(json);
        return obj.getMap();
    }

    public void flashParams() {
        // FIXME: different for GET?
        // FIXME: multiple values?
        CurrentVertxRequest currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) currentVertxRequest
                .getOtherHttpContextObject();
        FormData formData = rrContext.getFormData();
        if (formData != null) {
            for (String key : formData) {
                Deque<FormValue> values = formData.get(key);
                if (values.size() != 1) {
                    List<String> list = new ArrayList<>(values.size());
                    for (FormValue val : values) {
                        // skip files, since we can't set them in error forms anyway
                        if (!val.isFileItem()) {
                            list.add(val.getValue());
                        }
                        futureValues.put(key, list);
                    }
                } else {
                    FormValue firstValue = formData.getFirst(key);
                    // skip files, since we can't set them in error forms anyway
                    if (!firstValue.isFileItem()) {
                        futureValues.put(key, firstValue.getValue());
                    }
                }
            }
        }
    }

    public void flash(String key, Object value) {
        // FIXME: toString the value? unless it's a List, which we allow
        futureValues.put(key, value);
    }

    public <T> T get(String key) {
        // FIXME: is this really about the previous values or the future ones?
        return (T) values.get(key);
    }

    public Map<String, Object> values() {
        return values;
    }
}
