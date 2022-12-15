package io.quarkiverse.renarde;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkiverse.renarde.util.RenderArgs;
import io.quarkiverse.renarde.util.Validation;
import io.quarkus.security.identity.SecurityIdentity;

public class Controller {

    @Inject
    protected SecurityIdentity identity;

    @Inject
    protected RenderArgs renderArgs;

    @Inject
    protected Validation validation;

    @Inject
    protected Flash flash;

    // FIXME: force injecting it so that it exists in the request context because we use it in the templates
    // via the injection API
    @Inject
    protected UriInfo uriInfo;

    protected boolean validationFailed() {
        if (validation.hasErrors()) {
            prepareForErrorRedirect();
            return true;
        }
        return false;
    }

    protected void prepareForErrorRedirect() {
        flash.flashParams(); // add http parameters to the flash scope
        validation.keep(); // keep the errors for the next request
    }

    protected void flash(String key, Object value) {
        flash.flash(key, value);
    }

    protected static String emptyAsNull(String val) {
        if (val == null || val.isEmpty())
            return null;
        return val;
    }

    protected Response forbidden() {
        throw new WebApplicationException(RestResponse.StatusCode.FORBIDDEN);
    }

    protected Response forbidden(String message) {
        throw new WebApplicationException(
                RestResponse.ResponseBuilder.create(RestResponse.Status.FORBIDDEN, message).build().toResponse());
    }

    protected Response badRequest() {
        throw new WebApplicationException(RestResponse.StatusCode.BAD_REQUEST);
    }

    protected void notFoundIfNull(Object obj) {
        if (obj == null)
            throw new WebApplicationException(RestResponse.notFound().toResponse());
    }

    protected Response notFound(String message) {
        throw new WebApplicationException(
                RestResponse.ResponseBuilder.create(RestResponse.Status.NOT_FOUND, message).build().toResponse());
    }

    protected Response notFound() {
        throw new WebApplicationException(RestResponse.StatusCode.NOT_FOUND);
    }

    /**
     * This hook is called before any redirects caused by calls to controller public methods,
     * including redirect(FooController.class).method().
     */
    protected void beforeRedirect() {
    }

    protected Response seeOther(String uri) {
        try {
            return seeOther(new URI(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // static to allow usage in redirect(FooController.class).method() usage without actually having an instance of FooController
    protected static Response seeOther(URI uri) {
        throw new RedirectException(Response.seeOther(uri).build());
    }

    protected Response temporaryRedirect(URI uri) {
        throw new WebApplicationException(Response.temporaryRedirect(uri).build());
    }

    protected <T extends Controller> T redirect(Class<T> target) {
        throw new RuntimeException(
                "This method can only be called when instrumented together with a view call to the result directly: redirect(FooController.class).method()");
    }
}
