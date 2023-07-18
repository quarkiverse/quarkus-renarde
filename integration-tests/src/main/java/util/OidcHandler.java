package util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.renarde.oidc.RenardeOidcHandler;
import io.quarkiverse.renarde.oidc.RenardeOidcSecurity;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.RedirectException;
import model.User;
import rest.Application;

@ApplicationScoped
public class OidcHandler implements RenardeOidcHandler {

    @Inject
    Flash flash;

    @Inject
    RenardeOidcSecurity oidcSecurity;

    @Transactional
    @Override
    public void oidcSuccess(String tenantId, String authId) {
        flash.flash("message", "Welcome from OIDC for tenant " + tenantId + ", authId: " + authId
                + ", firstname: " + oidcSecurity.getOidcFirstName()
                + ", lastname: " + oidcSecurity.getOidcLastName()
                + ", username: " + oidcSecurity.getOidcUserName()
                + ", email: " + oidcSecurity.getOidcEmail());
        User user = new User();
        user.tenantId = tenantId;
        user.authId = authId;
        user.username = oidcSecurity.getOidcUserName();
        user.persist();
        throw new RedirectException(Response.seeOther(Router.getAbsoluteURI(Application::oidcWelcome)).build());

    }

    @Override
    public void loginWithOidcSession(String tenantId, String authId) {
        // not tested
    }

}
