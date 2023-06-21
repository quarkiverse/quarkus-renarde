package rest;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.pdf.Pdf;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.Produces;

public class PdfController extends Controller {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index();
        public static native TemplateInstance defaultPageSize();
    }

    @Produces(Pdf.APPLICATION_PDF)
    public TemplateInstance index() {
        return Templates.index();
    }

    @Produces(Pdf.APPLICATION_PDF)
    public TemplateInstance defaultPageSize() {
        return Templates.defaultPageSize();
    }
}
