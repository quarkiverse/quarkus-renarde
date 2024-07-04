package io.quarkiverse.renarde.barcode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.Code93Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.oned.UPCEWriter;
import com.google.zxing.qrcode.QRCodeWriter;

public class Barcode {

    public static BitMatrix code128(String value, int width, int height) {
        Code128Writer writer = new Code128Writer();
        return writer.encode(value, BarcodeFormat.CODE_128, width, height, getHints());
    }

    public static String code128Img(String value, int width, int height) {
        return dataUriImg(code128(value, width, height));
    }

    public static BitMatrix code39(String value, int width, int height) {
        Code39Writer writer = new Code39Writer();
        return writer.encode(value, BarcodeFormat.CODE_39, width, height, getHints());
    }

    public static String code39Img(String value, int width, int height) {
        return dataUriImg(code39(value, width, height));
    }

    public static BitMatrix code93(String value, int width, int height) {
        Code93Writer writer = new Code93Writer();
        return writer.encode(value, BarcodeFormat.CODE_93, width, height, getHints());
    }

    public static String code93Img(String value, int width, int height) {
        return dataUriImg(code93(value, width, height));
    }

    public static BitMatrix ean13(String value, int width, int height) {
        EAN13Writer writer = new EAN13Writer();
        return writer.encode(value, BarcodeFormat.EAN_13, width, height, getHints());
    }

    public static String ean13Img(String value, int width, int height) {
        return dataUriImg(ean13(value, width, height));
    }

    public static BitMatrix ean8(String value, int width, int height) {
        EAN8Writer writer = new EAN8Writer();
        return writer.encode(value, BarcodeFormat.EAN_8, width, height, getHints());
    }

    public static String ean8Img(String value, int width, int height) {
        return dataUriImg(ean8(value, width, height));
    }

    public static BitMatrix upcA(String value, int width, int height) {
        UPCAWriter writer = new UPCAWriter();
        return writer.encode(value, BarcodeFormat.UPC_A, width, height, getHints());
    }

    public static String upcAImg(String value, int width, int height) {
        return dataUriImg(upcA(value, width, height));
    }

    public static BitMatrix upcE(String value, int width, int height) {
        UPCEWriter writer = new UPCEWriter();
        return writer.encode(value, BarcodeFormat.UPC_E, width, height, getHints());
    }

    public static String upcEImg(String value, int width, int height) {
        return dataUriImg(upcE(value, width, height));
    }

    public static BitMatrix qrCode(String value, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            return writer.encode(value, BarcodeFormat.QR_CODE, width, height, getHints());
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    public static String qrCodeImg(String value, int width, int height) {
        return dataUriImg(qrCode(value, width, height));
    }

    public static BitMatrix dataMatrix(String value, int width, int height) {
        DataMatrixWriter writer = new DataMatrixWriter();
        return writer.encode(value, BarcodeFormat.DATA_MATRIX, width, height, getHints());
    }

    public static String dataMatrixImg(String value, int width, int height) {
        return dataUriImg(dataMatrix(value, width, height));
    }

    public static String dataUriImg(BitMatrix encoded) {
        return dataUriImg(base64ToDataUri(pngToBase64(barcodetoPng(encoded))));
    }

    public static byte[] barcodetoPng(BitMatrix encoded) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(encoded, "png", out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    public static String pngToBase64(byte[] png) {
        return Base64.getEncoder().encodeToString(png);
    }

    public static String base64ToDataUri(String base64) {
        return "data:image/png;base64," + base64;
    }

    public static String dataUriImg(String dataUri) {
        return "<img src='" + dataUri + "'/>";
    }

    private static Map<EncodeHintType, ?> getHints() {
        return Map.of(EncodeHintType.CHARACTER_SET, "UTF-8");
    }
}
