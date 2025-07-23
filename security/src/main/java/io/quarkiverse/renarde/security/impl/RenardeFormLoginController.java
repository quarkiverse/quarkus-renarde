package io.quarkiverse.renarde.security.impl;

import java.net.URI;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.configuration.RenardeConfig;
import io.quarkiverse.renarde.security.LoginPage;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkiverse.renarde.security.RenardeUserProvider;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkiverse.renarde.util.RenardeJWTAuthMechanism;
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
        public static native TemplateInstance login(String redirectQueryParam);
    }

    @LoginPage
    public TemplateInstance login(@QueryParam(RenardeJWTAuthMechanism.REDIRECT_URI) Optional<URI> redirectQueryParam) {
        return Templates.login(redirectQueryParam.map(URI::toASCIIString).orElse(null));
    }

    //
    // NOTE: public field injection is required in this class due to this class being in a different CL as the generated bean,
    // leading to access errors otherwise with package-protected

    @Inject
    public RenardeSecurity security;
    @Inject
    public RenardeUserProvider userProvider;

    @ConfigProperty(name = "quarkus.renarde.auth.redirect.cookie")
    public String redirectLocationCookie;

    @ConfigProperty(name = "quarkus.renarde.auth.redirect.type")
    RenardeConfig.RenardeAuthConfig.Redirect.Type redirectType;

    @Inject
    public HttpHeaders httpHeaders;

    @POST
    public Response login(@NotBlank @RestForm String username,
            @NotBlank @RestForm String password,
            @RestForm(RenardeJWTAuthMechanism.REDIRECT_URI) Optional<URI> redirectQueryParam) {
        if (validationFailed())
            login(redirectQueryParam);
        RenardeUserWithPassword user = (RenardeUserWithPassword) userProvider.findUser("manual", username);
        if (user == null) {
            validation.addError("username", "Unknown user");
        }
        if (validationFailed())
            login(redirectQueryParam);
        if (!BcryptUtil.matches(password, user.password())) {
            // invalid credentials, but hide it to not reveal account exists
            validation.addError("username", "Unknown user");
        }
        if (validationFailed())
            login(redirectQueryParam);
        NewCookie cookie = security.makeUserCookie(user);
        URI target;

        if (redirectType == RenardeConfig.RenardeAuthConfig.Redirect.Type.cookie) {
            Cookie quarkusRedirectLocation = httpHeaders.getCookies().get(redirectLocationCookie);
            target = URI.create(quarkusRedirectLocation != null ? quarkusRedirectLocation.getValue() : "/");
        } else {
            target = redirectQueryParam.orElse(URI.create("/"));
        }

        return Response.seeOther(target).cookie(cookie).build();
    }
}
