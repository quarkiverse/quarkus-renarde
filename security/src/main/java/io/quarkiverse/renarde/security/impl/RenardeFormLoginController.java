package io.quarkiverse.renarde.security.impl;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.security.LoginPage;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;

// FIXME: for now we only support ORM which is blocking
@Blocking
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

    @ConfigProperty(name = "quarkus.renarde.auth.location-cookie")
    String redirectLocationCookie;

    @Inject
    HttpHeaders httpHeaders;

    @POST
    public Response login(@NotBlank @RestForm String username,
            @NotBlank @RestForm String password) {
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
        Cookie quarkusRedirectLocation = httpHeaders.getCookies().get(redirectLocationCookie);
        String target = quarkusRedirectLocation != null ? quarkusRedirectLocation.getValue() : "/";
        return Response.seeOther(URI.create(target)).cookie(cookie).build();
    }
}
