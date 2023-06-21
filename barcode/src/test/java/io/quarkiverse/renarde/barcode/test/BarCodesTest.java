package io.quarkiverse.renarde.barcode.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import io.quarkiverse.renarde.barcode.runtime.QuteCode128Code;
import io.quarkiverse.renarde.barcode.runtime.QuteCode39Code;
import io.quarkiverse.renarde.barcode.runtime.QuteCode93Code;
import io.quarkiverse.renarde.barcode.runtime.QuteDataMatrix;
import io.quarkiverse.renarde.barcode.runtime.QuteEan13Code;
import io.quarkiverse.renarde.barcode.runtime.QuteEan8Code;
import io.quarkiverse.renarde.barcode.runtime.QuteQrCode;
import io.quarkiverse.renarde.barcode.runtime.QuteUpcACode;
import io.quarkiverse.renarde.barcode.runtime.QuteUpcECode;
import io.quarkus.qute.Engine;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;

public class BarCodesTest {
    private static final String PREFIX = "<img src='data:image/png;base64,";
    private static final String SUFFIX = "'/>";

    @Test
    public void qrCode() throws IOException, NotFoundException {
        testBarcode(new QuteQrCode(), "qrcode", "STEF", BarcodeFormat.QR_CODE);
    }

    @Test
    public void dataMatrix() throws IOException, NotFoundException {
        testBarcode(new QuteDataMatrix(), "datamatrix", "STEF", BarcodeFormat.DATA_MATRIX);
    }

    @Test
    public void ean13() throws IOException, NotFoundException {
        testBarcode(new QuteEan13Code(), "ean13", "9780201379624", BarcodeFormat.EAN_13);
    }

    @Test
    public void ean8() throws IOException, NotFoundException {
        testBarcode(new QuteEan8Code(), "ean8", "97802013", BarcodeFormat.EAN_8);
    }

    @Test
    public void code39() throws IOException, NotFoundException {
        testBarcode(new QuteCode39Code(), "code39", "9780201379624", BarcodeFormat.CODE_39);
    }

    @Test
    public void code93() throws IOException, NotFoundException {
        testBarcode(new QuteCode93Code(), "code93", "9780201379624", BarcodeFormat.CODE_93);
    }

    @Test
    public void code128() throws IOException, NotFoundException {
        testBarcode(new QuteCode128Code(), "code128", "9780201379624", BarcodeFormat.CODE_128);
    }

    @Test
    public void upcA() throws IOException, NotFoundException {
        testBarcode(new QuteUpcACode(), "upca", "978020137962", BarcodeFormat.UPC_A);
    }

    @Test
    public void upcE() throws IOException, NotFoundException {
        testBarcode(new QuteUpcECode(), "upce", "01234565", BarcodeFormat.UPC_E);
    }

    private void testBarcode(SectionHelperFactory<?> helper, String name, String validValue, BarcodeFormat format)
            throws IOException, NotFoundException {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(helper).build();

        Template template = engine.parse("{#" + name + " value='" + validValue + "'/}");
        assertBarcode(template, validValue, 200, 200, format);

        template = engine.parse("{#" + name + " '" + validValue + "'/}");
        assertBarcode(template, validValue, 200, 200, format);

        template = engine.parse("{#let f='" + validValue + "'}{#" + name + " f/}{/let}");
        assertBarcode(template, validValue, 200, 200, format);

        template = engine.parse("{#" + name + " value='" + validValue + "' width=300/}");
        assertBarcode(template, validValue, 300, 200, format);

        template = engine.parse("{#" + name + " value='" + validValue + "' height=300/}");
        assertBarcode(template, validValue, 200, 300, format);

        template = engine.parse("{#" + name + " value='" + validValue + "' width=300 height=400/}");
        assertBarcode(template, validValue, 300, 400, format);

        template = engine.parse("{#" + name + " value='" + validValue + "' size=300/}");
        assertBarcode(template, validValue, 300, 300, format);

        // missing value
        Assertions.assertThrows(TemplateException.class, () -> engine.parse("{#" + name + "/}"));
        // can't set both size and width/height
        Assertions.assertThrows(TemplateException.class,
                () -> engine.parse("{#" + name + " '" + validValue + "' size=100 width=300/}"));
        Assertions.assertThrows(TemplateException.class,
                () -> engine.parse("{#" + name + " '" + validValue + "' size=100 height=300/}"));
        Assertions.assertThrows(TemplateException.class,
                () -> engine.parse("{#" + name + " '" + validValue + "' size=100 width=300 height=300/}"));

        // invalid types
        Assertions.assertThrows(TemplateException.class, () -> engine.parse("{#" + name + " 1234/}").render());
        Assertions.assertThrows(TemplateException.class, () -> engine.parse("{#let f=1234}{#" + name + " f/}{/let}").render());
        Assertions.assertThrows(TemplateException.class,
                () -> engine.parse("{#" + name + " '" + validValue + "' size='asd'/}").render());
        // error on extra parameter
        Assertions.assertThrows(TemplateException.class,
                () -> engine.parse("{#" + name + " '" + validValue + "' unknown=23/}"));
    }

    private void assertBarcode(Template template, String text, int width, int height, BarcodeFormat format)
            throws IOException, NotFoundException {
        String rendered = template.render();
        Assertions.assertTrue(rendered.startsWith(PREFIX));
        Assertions.assertTrue(rendered.endsWith(SUFFIX));
        String base64 = rendered.substring(PREFIX.length(), rendered.length() - SUFFIX.length());
        byte[] bytes = Base64.getDecoder().decode(base64);

        // make sure it's PNG
        ImageReader imageReader = ImageIO.getImageReadersByFormatName("png").next();
        imageReader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)), true);

        BufferedImage image = imageReader.read(0);

        if (format == BarcodeFormat.CODE_39) {
            Assertions.assertTrue(width <= image.getWidth());
            Assertions.assertTrue(width + 10 > image.getWidth());
            Assertions.assertTrue(height <= image.getHeight());
            Assertions.assertTrue(height + 10 > image.getHeight());
        } else {
            Assertions.assertEquals(width, image.getWidth());
            Assertions.assertEquals(height, image.getHeight());
        }
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        // Try reading the barcode except for Data Matrix and UPC E
        if (format != BarcodeFormat.DATA_MATRIX
                && format != BarcodeFormat.UPC_E) {
            Result result = new MultiFormatReader().decode(binaryBitmap);
            Assertions.assertEquals(format, result.getBarcodeFormat());
            Assertions.assertEquals(text, result.getText());
        }
    }
}
