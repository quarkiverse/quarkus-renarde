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
public abstract class HxController extends Controller {

    public static final String HX_REQUEST_HEADER = "HX-Request";

    public enum HxResponseHeader {
        TRIGGER("HX-Trigger"), // Allows you to trigger client side events
        REDIRECT("HX-Redirect"), // Can be used to do a client-side redirect to a new location
        LOCATION("HX-Location"), // Allows you to do a client-side redirect that does not do a full page reload
        REFRESH("HX-Refresh"), // If set to “true” the client side will do a full refresh of the page
        PUSH_URL("HX-Push-Url"), // Pushes a new url into the history stack
        HX_RESWAP("HX-Reswap"), // Allows you to specify how the response will be swapped. See hx-swap for possible values
        HX_RETARGET("HX-Retarget"), // A CSS selector that updates the target of the content update to a different element on the page
        TRIGGER_AFTER_SWAP("HX-Trigger-After-Swap"), // Allows you to trigger client side events
        TRIGGER_AFTER_SETTLE("HX-Trigger-After-Settle"), // Allows you to trigger client side events
        REPLACE_URL("HX-Replace-Url"), // Replaces the current URL in the location bar
        RESELECT("HX-Reselect"); // A CSS selector that allows you to choose which part of the response is used to be swapped in. Overrides an existing hx-select on the triggering element

        private final String key;

        HxResponseHeader(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum HxRequestHeader {
        BOOSTED("HX-Boosted"), // Indicates that the request is via an element using hx-boost
        CURRENT_URL("HX-Current-URL"), // The current URL of the browser
        HISTORY_RESTORE_REQUEST("HX-History-Restore-Request"), // true if the request is for history restoration after a miss in the local history cache
        PROMPT("HX-Prompt"), // The user response to an hx-prompt
        REQUEST("HX-Request"), // Always true
        TARGET("HX-Target"), // The id of the target element if it exists
        TRIGGER_NAME("HX-Trigger-Name"), // The name of the triggered element if it exists
        TRIGGER("HX-Trigger"); // The id of the triggered element if it exists

        private final String key;

        HxRequestHeader(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
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

    protected String hx(HxRequestHeader header) {
        return this.httpHeaders.getHeaderString(header.key);
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
