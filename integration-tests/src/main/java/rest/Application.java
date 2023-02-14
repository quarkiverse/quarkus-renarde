package rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import model.User;

public class Application extends Controller {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance routingTags();

        public static native TemplateInstance validatedForm();

        public static native TemplateInstance test(User user);
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

    public String router() {
        return Router.getURI(Application::absolutePath)
                + "\n" + Router.getURI(Application::index)
                + "\n" + Router.getURI(Application::params, "first", 42l, "search")
                + "\n" + Router.getURI(Application::primitiveParams, true, 'a', (byte) 2, (short) 3, 4, 5l, 6.0f, 7.0d);
    }

    @POST
    public void setupUser() {
        User.deleteAll();
        User user = new User();
        user.username = "FroMage";
        user.password = BcryptUtil.bcryptHash("1q2w3e");
        user.persistAndFlush();
    }
}
