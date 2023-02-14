package io.quarkiverse.renarde.util;

import java.util.Base64;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.core.multipart.FormData.FormValue;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;

@Named("CRSF")
@RequestScoped
public class CRSF {

    @Inject
    HttpServerRequest request;

    private String crsfToken;

    private final static int CRSF_SIZE = 16;
    private final static String CRSF_COOKIE_NAME = "_renarde_crsf";
    private final static String CRSF_FORM_NAME = "_renarde_crsf_token";

    public void setCRSFCookie() {
        // FIXME: expiry, others?
        // in some cases with exception mappers, it appears the filters get invoked twice
        // FIXME: sometimes we seem to lose the flow and request scope, leading to calls like:
        /*
         * Reading CRSF cookie
         * Existing cookie: MEJBRQDw9Y8FGOmEG1vItA==
         * Saving CRSF cookie: MEJBRQDw9Y8FGOmEG1vItA==
         * Saving CRSF cookie: null
         */
        if (!request.response().headWritten() && crsfToken != null)
            request.response().addCookie(
                    Cookie.cookie(CRSF_COOKIE_NAME, crsfToken).setPath("/"));
    }

    public void readCRSFCookie() {
        Cookie cookie = request.getCookie(CRSF_COOKIE_NAME);
        if (cookie != null) {
            crsfToken = cookie.getValue();
        } else {
            byte[] bytes = new byte[CRSF_SIZE];
            RandomHolder.SECURE_RANDOM.nextBytes(bytes);
            crsfToken = Base64.getEncoder().encodeToString(bytes);
        }
    }

    public void checkCRSFToken() {
        CurrentVertxRequest currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) currentVertxRequest
                .getOtherHttpContextObject();
        FormData formData = rrContext.getFormData();
        String formToken = null;
        // FIXME: we could allow checks for query params
        if (formData != null) {
            FormValue value = formData.getFirst(CRSF_FORM_NAME);
            formToken = value != null ? value.getValue() : null;
        }
        if (formToken == null || !formToken.equals(crsfToken)) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity("Invalid or missing CRSF Token").build());
        }
    }

    // For views
    public String formName() {
        return CRSF_FORM_NAME;
    }

    // For views
    public String token() {
        return crsfToken;
    }
}
