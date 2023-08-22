package rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import model.User;

public class Application extends Controller {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance oidcWelcome();

        public static native TemplateInstance routingTags();

        public static native TemplateInstance validatedForm();

        public static native TemplateInstance test(User user);

        public static native TemplateInstance csrf();
    }

    @POST
    public String form(@RestForm String param,
            @RestForm File file,
            @RestForm FileUpload fileUpload) throws IOException {
        return "param: " + param + ", file: " + Files.readString(file.toPath()) + ", fileUpload: "
                + fileUpload.fileName();
    }

    public String hello() {
        return "Hello Renarde";
    }

    public TemplateInstance test() {
        User user = new User();
        user.roles = "a, b";
        return Templates.test(user);
    }

    public TemplateInstance index() {
        return Templates.index();
    }

    @Blocking
    public TemplateInstance oidcWelcome() {
        return Templates.oidcWelcome();
    }

    public TemplateInstance routingTags() {
        return Templates.routingTags();
    }

    public TemplateInstance validatedForm() {
        return Templates.validatedForm();
    }

    // FIXME: I'm not sure why form() works but this doesn't. I'm getting an error because it's called on the IO thread
    @Transactional(TxType.NEVER)
    @POST
    public TemplateInstance validatedAction(@RestForm @NotBlank String required,
            @RestForm @Email String email,
            @RestForm String manual,
            @RestForm boolean redirect) {
        validation.required("manual", manual);
        // only flash the errors and params if we test redirects
        if (redirect && validationFailed()) {
            validatedForm();
        }
        // make sure errors show up even without a redirect
        return Templates.validatedForm();
    }

    @Path("/absolute")
    public String absolutePath() {
        return "Absolute";
    }

    public String params(@RestPath String a, @RestPath Long id, @RestQuery String q) {
        return "Got params: " + a + "/" + id + "/" + q;
    }

    public String primitiveParams(@RestQuery boolean b,
            @RestQuery char c,
            @RestQuery byte bite,
            @RestQuery short s,
            @RestQuery int i,
            @RestQuery long l,
            @RestQuery float f,
            @RestQuery double d) {
        return "Got params: " + b + "/" + c + "/" + bite + "/" + s + "/" + i + "/" + l + "/" + f + "/" + d;
    }

    public String optionalParams(@RestQuery Optional<Long> id, @RestQuery Optional<Long> number) {
        return "Got params: " + id + "/" + number;
    }

    public String routerRedirect() {
        return extractRedirect(() -> absolutePath())
                + "\n" + extractRedirect(() -> index())
                + "\n" + extractRedirect(() -> params("first", 42l, "search"))
                + "\n" + extractRedirect(() -> primitiveParams(true, 'a', (byte) 2, (short) 3, 4, 5l, 6.0f, 7.0d))
                + "\n" + extractRedirect(() -> optionalParams(Optional.of(42l), Optional.empty()));
    }

    public String routerRedirect2() {
        return extractRedirect(() -> absolutePath())
                + "\n" + extractRedirect(() -> redirect(Application.class).index())
                + "\n" + extractRedirect(() -> redirect(Application.class).params("first", 42l, "search"))
                + "\n"
                + extractRedirect(
                        () -> redirect(Application.class).primitiveParams(true, 'a', (byte) 2, (short) 3, 4, 5l, 6.0f, 7.0d))
                + "\n" + extractRedirect(() -> redirect(Application.class).optionalParams(Optional.of(42l), Optional.empty()));
    }

    private String extractRedirect(Runnable run) {
        try {
            run.run();
        } catch (RedirectException x) {
            return x.response.getHeaderString("Location");
        }
        return null;
    }

    public String router() {
        return Router.getURI(Application::absolutePath)
                + "\n" + Router.getURI(Application::index)
                + "\n" + Router.getURI(Application::params, "first", 42l, "search")
                + "\n" + Router.getURI(Application::primitiveParams, true, 'a', (byte) 2, (short) 3, 4, 5l, 6.0f, 7.0d)
                + "\n" + Router.getURI(Application::optionalParams, Optional.of(42l), Optional.empty());
    }

    public TemplateInstance csrf() {
        return Templates.csrf();
    }

    @Inject
    CsrfTokenParameterProvider csrfToken;

    public String csrfToken() {
        return csrfToken.getToken();
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public String csrfForm1(@RestForm String name) {
        return "OK: " + name;
    }

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    public String csrfForm2(@RestForm String name) {
        return "OK: " + name;
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public String csrfForm3() {
        return "OK";
    }

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    public String csrfForm4() {
        return "OK";
    }
}
