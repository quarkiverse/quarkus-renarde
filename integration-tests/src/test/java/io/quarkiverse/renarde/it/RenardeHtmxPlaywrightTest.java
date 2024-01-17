package io.quarkiverse.renarde.it;

import java.net.URL;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import service.ContactService;

@QuarkusTest
@WithPlaywright(verbose = true)
public class RenardeHtmxPlaywrightTest {

    @Inject
    ContactService contactService;

    @InjectPlaywright
    BrowserContext context;

    @TestHTTPResource("/HtmxApp")
    URL index;

    @Test
    public void testOpenAndEditJoeAndBar() {
        contactService.reset();
        try (Page page = context.newPage()) {
            Response response = page.navigate(index.toString());
            Assertions.assertEquals("OK", response.statusText());

            page.waitForLoadState();

            Assertions.assertEquals("Htmx Powered", page.title());

            page.waitForSelector("[aria-label='Viewing Foo BLOW'] button[aria-label='Edit']").click();
            final ElementHandle editingFooBlow = page.waitForSelector("[aria-label='Editing Foo BLOW']");

            page.waitForSelector("[aria-label='Viewing Joe BLOW'] button[aria-label='Edit']").click();
            final ElementHandle editingJoeBlow = page.waitForSelector("[aria-label='Editing Joe BLOW']");

            editingFooBlow.waitForSelector("input[name='firstName']").fill("Foofoo");
            editingFooBlow.waitForSelector("input[name='lastName']").fill("BLOWBLOW");
            editingFooBlow.waitForSelector("button[aria-label='Save']").click();

            editingJoeBlow.waitForSelector("input[name='firstName']").fill("Joejoe");
            editingJoeBlow.waitForSelector("input[name='lastName']").fill("BLOWBLOW");
            editingJoeBlow.waitForSelector("button[aria-label='Save']").click();

            checkContact(page, new ContactService.Contact(6, "Joejoe", "BLOWBLOW", "joe@blow.io"));
            checkContact(page, new ContactService.Contact(7, "Foofoo", "BLOWBLOW", "foo@blow.io"));
        }
    }

    @Test
    public void testLockJoe() {
        contactService.reset();
        try (Page page = context.newPage()) {
            Response response = page.navigate(index.toString());
            Assertions.assertEquals("OK", response.statusText());
            page.waitForLoadState();

            page.waitForSelector("[aria-label='Viewing Joe BLOW'] button[aria-label='Edit']").click();
            final ElementHandle editingJoeBlow = page.waitForSelector("[aria-label='Editing Joe BLOW']");
            editingJoeBlow.waitForSelector("button[aria-label='Lock']").click();
            page.waitForSelector("[aria-label='Viewing Joe BLOW'] button[aria-label='Edit']").click();
            final ElementHandle editingJoeBlow2 = page.waitForSelector("[aria-label='Editing Joe BLOW']");
            Assertions.assertFalse(editingJoeBlow2.waitForSelector("input[name='firstName']").isEditable());
            Assertions.assertFalse(editingJoeBlow2.waitForSelector("input[name='lastName']").isEditable());
            Assertions.assertFalse(editingJoeBlow2.waitForSelector("input[name='email']").isEditable());
            editingJoeBlow2.waitForSelector("button[aria-label='Unlock']").click();
            // Remove the two next lines to reproduce the flash bug (it will fail in testDeleteJoe when running all tests)
            page.waitForCondition(
                    () -> page.waitForSelector("[aria-label='Editing Joe BLOW'] input[name='firstName']").isEditable());
        }
    }

    @Test
    public void testDeleteJoe() {
        contactService.reset();
        try (Page page = context.newPage()) {
            Response response = page.navigate(index.toString());
            Assertions.assertEquals("OK", response.statusText());
            page.waitForLoadState();

            page.waitForSelector("[aria-label='Viewing Joe BLOW'] button[aria-label='Edit']").click();
            final ElementHandle editingJoeBlow = page.waitForSelector("[aria-label='Editing Joe BLOW']");
            editingJoeBlow.waitForSelector("button[aria-label='Delete']").click();

            page.waitForCondition(() -> page.querySelector("[aria-label='Viewing Joe BLOW']") == null);
            Assertions.assertEquals(2, contactService.contacts().size());
        }
    }

    @Test
    public void testFormValidation() {
        contactService.reset();
        try (Page page = context.newPage()) {
            Response response = page.navigate(index.toString());
            Assertions.assertEquals("OK", response.statusText());
            page.waitForLoadState();

            page.waitForSelector("[aria-label='Viewing Joe BLOW'] button[aria-label='Edit']").click();
            final ElementHandle editingJoeBlow = page.waitForSelector("[aria-label='Editing Joe BLOW']");
            editingJoeBlow.waitForSelector("input[name='firstName']").fill("");
            editingJoeBlow.waitForSelector("input[name='lastName']").fill("blow");
            editingJoeBlow.waitForSelector("button[aria-label='Save']").click();

            page.waitForSelector("[aria-label='Editing Joe BLOW'] input.is-invalid[name='firstName']");
            page.waitForSelector("[aria-label='Editing Joe BLOW'] input.is-invalid[name='lastName']");
            page.waitForSelector("[aria-label='Editing Joe BLOW'] [aria-label='Error for firstName']");
            page.waitForSelector("[aria-label='Editing Joe BLOW'] [aria-label='Error for lastName']");
        }
    }

    private static void checkContact(Page page, ContactService.Contact contact) {
        final String fooContent = page.waitForSelector(
                "[aria-label='Viewing " + contact.firstName + " " + contact.lastName + "'] [aria-label='Details']")
                .textContent();
        Assertions.assertEquals(
                "First Name: " + contact.firstName + " Last Name: " + contact.lastName + " Email: " + contact.email,
                fooContent.strip().replaceAll("\\s+", " "));
    }

}
