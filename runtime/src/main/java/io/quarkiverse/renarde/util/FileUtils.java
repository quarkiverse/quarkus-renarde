package io.quarkiverse.renarde.util;

import jakarta.ws.rs.core.MediaType;

import org.overviewproject.mime_types.GetBytesException;
import org.overviewproject.mime_types.MimeTypeDetector;

public class FileUtils {
    private final static MimeTypeDetector DETECTOR = new MimeTypeDetector();

    /**
     * Returns a guessed mime type for the given file name and bytes
     *
     * @param filename the file name of the contents, pass an empty string to disable file name guessing
     * @param bytes the bytes to analyse
     * @return the guessed mime type, or "application/octet-stream" if it cannot be guessed
     */
    public static String getMimeType(String filename, byte[] bytes) {
        try {
            return DETECTOR.detectMimeType(filename, () -> bytes);
        } catch (GetBytesException e) {
            e.printStackTrace();
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
