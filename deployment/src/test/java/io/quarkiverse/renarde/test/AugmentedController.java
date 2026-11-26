package io.quarkiverse.renarde.test;

import java.net.URI;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;

/**
 * This class has the augmentation in ControllerVisitor done manually, to help illustrate it.
 */
public class AugmentedController extends Controller {

    public void otherAction() {
        // calling
        hello("foo", "bar");
        // is replaced by:
        this.beforeRedirect();
        AugmentedController.__redirect$hello("foo", "bar");

        // calling
        redirect(AugmentedController.class).hello("foo", "bar");
        // is replaced by
        this.beforeRedirect();
        AugmentedController.__redirect$hello("foo", "bar");

        // calling
        Router.getURI(AugmentedController::hello, new Object[] { "foo", "bar" });
        // is replaced by
        AugmentedController.__urivarargs$hello(false, "foo", "bar");

        // calling
        Router.getAbsoluteURI(AugmentedController::hello, new Object[] { "foo", "bar" });
        // is replaced by
        AugmentedController.__urivarargs$hello(true, "foo", "bar");
    }

    // --- Example 1: Simple method with only path and query params ---

    /**
     * Sample action with path param and query param
     */
    public String hello(@RestPath String id, @RestQuery String param) {
        return "asd";
    }

    /**
     * This triggers a redirect to the action. Does not return.
     */
    public static String __redirect$hello(String id, String param) {
        Controller.seeOther(AugmentedController.__uri$hello(true, id, param));
        return null;
    }

    /**
     * Generates a URI to the action, varargs variant, called from the views and Router.getURI substitution.
     * This supports calling less parameters, and relies on default values.
     */
    public static URI __urivarargs$hello(boolean absolute, Object... params) {
        return AugmentedController.__uri$hello(absolute,
                params.length > 0 ? (String) params[0] : null,
                params.length > 1 ? (String) params[1] : null);
    }

    /**
     * Generates a URI to the action.
     */
    public static URI __uri$hello(boolean absolute, String id, String param) {
        UriBuilder uri = Router.getUriBuilder(absolute);
        // static part
        uri.path("Controller");
        uri.path("hello");
        // path param uri part
        uri.path("{id}");
        uri.resolveTemplate("id", id);
        // query param part
        Object[] paramVals = new Object[1];
        paramVals[0] = param;
        uri.queryParam("param", paramVals);

        return uri.build();
    }

    // --- Example 2: Method with non-URI params (injected @Context, @RestForm) ---
    // Non-URI params are skipped in __urivarargs$ and receive null defaults.
    // Only @RestPath and @RestQuery params consume varargs slots.

    /**
     * Action with a @Context param (UriInfo) and a @RestForm param between path and query.
     */
    public String mixed(@RestPath String id, @Context UriInfo uriInfo, @RestForm String formField, @RestQuery String q) {
        return id + "|" + q;
    }

    /**
     * Redirect: forwards ALL params (including non-URI ones) to __uri$mixed,
     * which simply ignores them.
     */
    public static String __redirect$mixed(String id, UriInfo uriInfo, String formField, String q) {
        Controller.seeOther(AugmentedController.__uri$mixed(true, id, uriInfo, formField, q));
        return null;
    }

    /**
     * Varargs URI builder: only path/query params (id at index 0, q at index 1) consume varargs slots.
     * Non-URI params (uriInfo, formField) always get null.
     */
    public static URI __urivarargs$mixed(boolean absolute, Object... params) {
        return AugmentedController.__uri$mixed(absolute,
                // param 0 (id): URI param → varargs[0]
                params.length > 0 ? (String) params[0] : null,
                // param 1 (uriInfo): non-URI param → always null
                null,
                // param 2 (formField): non-URI param → always null
                null,
                // param 3 (q): URI param → varargs[1]
                params.length > 1 ? (String) params[1] : null);
    }

    /**
     * URI builder: takes all params in signature but only uses path/query ones.
     * The uriInfo and formField params are unused.
     */
    public static URI __uri$mixed(boolean absolute, String id, UriInfo uriInfo, String formField, String q) {
        UriBuilder uri = Router.getUriBuilder(absolute);
        uri.path("Controller");
        uri.path("mixed");
        uri.path("{id}");
        uri.resolveTemplate("id", id);
        if (q != null) {
            Object[] qVals = new Object[1];
            qVals[0] = q;
            uri.queryParam("q", qVals);
        }
        return uri.build();
    }
}
