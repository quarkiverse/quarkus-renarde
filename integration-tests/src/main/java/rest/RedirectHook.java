package rest;

import io.quarkiverse.renarde.Controller;

public class RedirectHook extends Controller {
    @Override
    protected void beforeRedirect() {
        System.err.println("beforeRedirect!");
        flash("flashed", "OK");
    }

    public void redirectHookDirect() {
        redirectHookTarget();
    }

    public void redirectHookIndirect() {
        redirect(RedirectHook.class).redirectHookTarget();
    }

    public String redirectHookTarget() {
        System.err.println("target: do we have some flash values? " + flash.values());
        return flash.get("flashed");
    }

}
