package io.quarkiverse.renarde.it;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.net.URL;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RenardePdfTest {

    @TestHTTPResource
    URL baseURI;

    @Test
    public void testPdf() throws IOException {
        byte[] bytes = given()
                .when().get("/PdfController/index")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .extract().body().asByteArray();
        PDDocument pdf = Loader.loadPDF(bytes);
        Assertions.assertEquals("Hello", pdf.getDocumentInformation().getTitle());
        Assertions.assertEquals(1, pdf.getNumberOfPages());
        PDRectangle mediaBox = pdf.getPage(0).getMediaBox();
        Assertions.assertEquals(150, pointsToMm(mediaBox.getWidth()));
        Assertions.assertEquals(90, pointsToMm(mediaBox.getHeight()));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(pdf);
        Assertions.assertEquals("Some PDF text.\nexternalCSS proof\ninternalCSS proof\n", text);
    }

    @Test
    public void testDefaultPageSize() throws IOException {
        byte[] bytes = given()
                .when().get("/PdfController/defaultPageSize")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .extract().body().asByteArray();
        PDDocument pdf = Loader.loadPDF(bytes);
        Assertions.assertEquals(1, pdf.getNumberOfPages());
        PDRectangle mediaBox = pdf.getPage(0).getMediaBox();
        Assertions.assertEquals(210, pointsToMm(mediaBox.getWidth()));
        Assertions.assertEquals(297, pointsToMm(mediaBox.getHeight()));
    }

    private float pointsToMm(float points) {
        return Math.round(points * 25.4f / 72f);
    }
}
