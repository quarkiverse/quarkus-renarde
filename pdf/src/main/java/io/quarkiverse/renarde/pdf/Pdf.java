package io.quarkiverse.renarde.pdf;

import jakarta.ws.rs.core.MediaType;

public class Pdf {

    public final static String APPLICATION_PDF = "application/pdf";
    public final static String IMAGE_PNG = "image/png";
    public final static MediaType APPLICATION_PDF_TYPE = new MediaType("application", "pdf");
    public final static MediaType IMAGE_PNG_TYPE = new MediaType("image", "png");
}
