package io.quarkiverse.renarde.security.impl;

import java.net.URI;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.LoginPage;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("_renarde/security")
public class RenardeFormLoginController extends Controller {
    @CheckedTemplate(basePath = "_renarde/security")
    public static class Templates {
        public static native TemplateInstance login();
    }

    @LoginPage
    public TemplateInstance login() {
        return Templates.login();
    }

    @Inject
    RenardeSecurity security;
    @Inject
    RenardeUserProvider userProvider;

    @POST
    public Response login(@NotBlank @RestForm String username,
            @NotBlank @RestForm String password,
            @RestCookie("quarkus-redirect-location") String quarkusRedirectLocation) {
        if (validationFailed())
            login();
        RenardeUserWithPassword user = (RenardeUserWithPassword) userProvider.findUser("manual", username);
        if (user == null) {
            validation.addError("username", "Unknown user");
        }
        if (validationFailed())
            login();
        if (!BcryptUtil.matches(password, user.password())) {
            // invalid credentials, but hide it to not reveal account exists
            validation.addError("username", "Unknown user");
        }
        if (validationFailed())
            login();
        NewCookie cookie = security.makeUserCookie(user);
        String target = quarkusRedirectLocation != null ? quarkusRedirectLocation : "/";
        return Response.seeOther(URI.create(target)).cookie(cookie).build();
    }
}
