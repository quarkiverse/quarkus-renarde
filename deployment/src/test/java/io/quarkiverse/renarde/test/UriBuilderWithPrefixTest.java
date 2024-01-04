package io.quarkiverse.renarde.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.renarde.Controller;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.test.QuarkusUnitTest;

public class UriBuilderWithPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestService.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/support"), "application.properties")
                    .addAsResource(new StringAsset(
                            "{uri:MyController.endpoint()}\n{uriabs:MyController.endpoint()}\n{uri:MyController.endpoint('param')}\n{uriabs:MyController.endpoint('param')}"),
                            "templates/MyController/email.html"));

    public static class MyController extends Controller {

        @CheckedTemplate
        public static class Templates {
            public static native MailTemplate.MailTemplateInstance email();
        }

        @Path("/endpoint-url")
        public void endpoint(@QueryParam("t") String testParam) {
        }
    }

    @Inject
    TestService testService;

    @Inject
    MockMailbox mockMailbox;

    @Test
    void testDefaultUriBuilderIsUsed() {
        testService.sendQuteEmail();

        var email = "none@icann.org";
        List<Mail> sent = mockMailbox.getMailsSentTo(email);
        assertEquals(1, sent.size(), "It has send one email");

        Mail mail = sent.get(0);
        assertEquals(email, mail.getTo().get(0), "Email should be " + email);

        String html = mail.getHtml();

        var splittedPaths = html.split("\n");
        var relativePath = splittedPaths[0];
        var absolutePath = splittedPaths[1];
        var relativePathWithParam = splittedPaths[2];
        var absolutePathWithParam = splittedPaths[3];

        assertEquals("/support/endpoint-url", relativePath);
        assertEquals("http://localhost:8081/support/endpoint-url", absolutePath);
        assertEquals("/support/endpoint-url?t=param", relativePathWithParam);
        assertEquals("http://localhost:8081/support/endpoint-url?t=param", absolutePathWithParam);
    }

    @ApplicationScoped
    public static class TestService {

        public void sendQuteEmail() {
            MyController.Templates
                    .email()
                    .to("none@icann.org")
                    .send()
                    .await()
                    .atMost(Duration.ofSeconds(10));
        }

    }

}
