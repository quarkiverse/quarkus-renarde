package io.quarkiverse.renarde.htmx;

import java.util.Arrays;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.vertx.core.http.HttpServerResponse;

/**
 * This is a Controller's overlay to simplify usage of Quarkus Renarde alongside {@see <a href="https://htmx.org/">htmx</a>}.
 */
public class HxController extends Controller {

    public static final String HX_REQUEST_HEADER = "HX-Request";

    public enum HxResponseHeader {
        // triggers client side events
        TRIGGER("HX-Trigger"),
        // triggers a client-side redirect to a new location
        REDIRECT("HX-Redirect"),
        // triggers a client-side redirect to a new location that acts as a swap
        LOCATION("HX-Location"),
        // if set to "true" the client side will do a full refresh of the page
        REFRESH("HX-Location"),
        // pushes a new URL into the browser’s address bar
        PUSH("HX-Push"),
        // triggers client side events after the swap step
        TRIGGER_AFTER_SWAP("HX-Trigger-After-Swap"),
        // triggers client side events after the settle step
        TRIGGER_AFTER_SETTLE("HX-Trigger-After-Settle");

        private final String key;

        HxResponseHeader(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    @Inject
    protected HttpHeaders httpHeaders;

    @Inject
    protected HttpServerResponse response;

    /**
     * This Qute helper make it easy to achieve htmx "Out of Band" swap by choosing which templates to return (refresh).
     * <br />
     * {@see <a href="https://htmx.org/attributes/hx-swap-oob/">Doc for htmx "hx-swap-oob"</a>}
     *
     * <br />
     *
     * @param templates the list of template instances to concatenate
     * @return the concatenated templates instances
     */
    public static TemplateInstance concatTemplates(TemplateInstance... templates) {
        return Qute.fmt("{#each elements}{it.raw}{/each}")
                .cache()
                .data("elements", Arrays.stream(templates).map(TemplateInstance::createUni))
                .instance();
    }

    /**
     * Check if this request has the htmx flag (header or flash data)
     */
    protected boolean isHxRequest() {
        final boolean hxRequest = Objects.equals(flash.get(HX_REQUEST_HEADER), true);
        if (hxRequest) {
            return true;
        }
        return Objects.equals(httpHeaders.getHeaderString(HX_REQUEST_HEADER), "true");
    }

    /**
     * Helper to define htmx response headers.
     *
     * @param hxHeader the {@link HxResponseHeader} to define
     * @param value the value for this header
     */
    protected void hx(HxResponseHeader hxHeader, String value) {
        response.headers().set(hxHeader.key(), value);
    }

    /**
     * Make sure only htmx requests are calling this method.
     */
    protected void onlyHxRequest() {
        if (!isHxRequest()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity("Only Hx request are allowed on this method").build());
        }
    }

    /**
     * Keep the htmx flag for the redirect request.
     * This is automatic.
     */
    protected void flashHxRequest() {
        flash(HX_REQUEST_HEADER, isHxRequest());
    }

    @Override
    protected void beforeRedirect() {
        flashHxRequest();
        super.beforeRedirect();
    }

}
