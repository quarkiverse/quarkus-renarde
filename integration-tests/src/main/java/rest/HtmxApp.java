package rest;

import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import service.ContactService;

public class HtmxApp extends HxController {

    @Inject
    ContactService contactService;

    @CheckedTemplate
    static class Templates {

        public static native TemplateInstance index(Collection<ContactService.Contact> contacts);

        public static native TemplateInstance index$list(Collection<ContactService.Contact> contacts);

    }

    @CheckedTemplate(basePath = "HtmxApp/partials")
    static class Partials {

        public static native TemplateInstance view(ContactService.Contact contact);

        public static native TemplateInstance edit(ContactService.Contact contact);

    }

    @Path("")
    public TemplateInstance index() {
        if (isHxRequest()) {
            return Templates.index$list(contactService.contacts().values());
        }
        return Templates.index(contactService.contacts().values());
    }

    public TemplateInstance view(@RestPath int id) {
        if (!contactService.contacts().containsKey(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        return Partials.view(contactService.contacts().get(id));
    }

    public TemplateInstance edit(@RestPath int id) {
        if (!contactService.contacts().containsKey(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        return Partials.edit(contactService.contacts().get(id));
    }

    @PUT
    public void save(@RestPath int id, @RestForm @NotBlank @Pattern(regexp = "[A-Z][a-z]+") String firstName,
            @RestForm @NotBlank @Pattern(regexp = "[A-Z]+") String lastName,
            @RestForm @Email String email) {
        if (!contactService.contacts().containsKey(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }

        if (validationFailed()) {
            edit(id);
        }

        final ContactService.Contact contact = contactService.contacts().get(id);
        contact.firstName = firstName;
        contact.lastName = lastName;
        contact.email = email;
        view(id);
    }

    @PUT
    public void lock(@RestPath int id) {
        if (!contactService.contacts().containsKey(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        final ContactService.Contact contact = contactService.contacts().get(id);
        contact.locked = !contact.locked;
        if (contact.locked) {
            view(id);
        } else {
            edit(id);
        }
    }

    @DELETE
    public void delete(@RestPath int id) {
        if (!contactService.contacts().containsKey(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        contactService.contacts().remove(id);
        hx(HxResponseHeader.TRIGGER, "refreshList");
    }

}
