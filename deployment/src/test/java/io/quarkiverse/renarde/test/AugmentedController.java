package io.quarkiverse.renarde.test;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

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
    }

    // FIXME: test with path param in @Path annotation
    // FIXME: test with injected param like UriInfo
    // FIXME: test with body param
    // FIXME: test with bean param

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
        Controller.seeOther(AugmentedController.__uri$hello(false, id, param));
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
}
