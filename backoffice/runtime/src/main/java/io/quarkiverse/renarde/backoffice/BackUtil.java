package io.quarkiverse.renarde.backoffice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.engine.jdbc.BlobProxy;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

public class BackUtil {

    public static Date dateField(String value) {
        if (!isSet(value))
            return null;
        // seconds part is optional
        try {
            return new SimpleDateFormat(JavaExtensions.HTML_NORMALISED_FORMAT).parse(value);
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat(JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS_FORMAT).parse(value);
            } catch (ParseException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public static LocalDateTime localDateTimeField(String value) {
        if (!isSet(value))
            return null;
        // seconds part is optional
        try {
            return LocalDateTime.parse(value, JavaExtensions.HTML_NORMALISED);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value, JavaExtensions.HTML_NORMALISED_WITHOUT_SECONDS);
        }
    }

    public static LocalDate localDateField(String value) {
        if (!isSet(value))
            return null;
        return LocalDate.parse(value, JavaExtensions.HTML_DATE);
    }

    public static LocalTime localTimeField(String value) {
        if (!isSet(value))
            return null;
        // seconds part is optional
        try {
            return LocalTime.parse(value, JavaExtensions.HTML_TIME);
        } catch (DateTimeParseException e) {
            return LocalTime.parse(value, JavaExtensions.HTML_TIME_WITHOUT_SECONDS);
        }
    }

    public static boolean booleanField(String value) {
        return value != null && value.equals("on");
    }

    public static byte byteField(String value) {
        return isSet(value) ? Byte.valueOf(value) : 0;
    }

    public static short shortField(String value) {
        return isSet(value) ? Short.valueOf(value) : 0;
    }

    public static int intField(String value) {
        return isSet(value) ? Integer.valueOf(value) : 0;
    }

    public static long longField(String value) {
        return isSet(value) ? Long.valueOf(value) : 0;
    }

    public static float floatField(String value) {
        return isSet(value) ? Float.valueOf(value) : 0;
    }

    public static double doubleField(String value) {
        return isSet(value) ? Double.valueOf(value) : 0;
    }

    public static char charField(String value) {
        if (isSet(value)) {
            if (value.length() != 1)
                throw new RuntimeException("Invalid character: " + value);
            return value.charAt(0);
        }
        return 0;
    }

    public static String stringField(String value) {
        // turn empty fields into null, otherwise we can't unset fields in the UI
        // turning them to null allows us to not run into unicity constraints that only apply
        // if non-null (like with empty strings)
        if (!isSet(value))
            return null;
        return value;
    }

    public static <T extends Enum<T>> T enumField(Class<T> klass, String value) {
        if (!isSet(value))
            return null;
        return Enum.valueOf(klass, value);
    }

    public static Map<String, String> enumPossibleValues(Enum<?>[] values) {
        Map<String, String> ret = new TreeMap<>();
        for (Enum<?> value : values) {
            ret.put(value.name(), value.name());
        }
        return ret;
    }

    public static Map<String, String> entityPossibleValues(List<PanacheEntity> list) {
        Map<String, String> ret = new TreeMap<>();
        for (PanacheEntity entity : list) {
            ret.put(String.valueOf(entity.id), entity.toString());
        }
        return ret;
    }

    public static List<String> entityCurrentValues(List<PanacheEntity> list) {
        List<String> ret = new ArrayList<>(list.size());
        for (PanacheEntity entity : list) {
            ret.add(String.valueOf(entity.id));
        }
        return ret;
    }

    public static boolean isSet(String value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isSet(FileUpload value) {
        return value != null && !value.fileName().isEmpty();
    }

    public static byte[] byteArrayField(FileUpload fileUpload) {
        if (!isSet(fileUpload))
            return null;
        try {
            return Files.readAllBytes(fileUpload.filePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Blob blobField(FileUpload fileUpload) {
        if (!isSet(fileUpload))
            return null;
        try {
            return BlobProxy.generateProxy(Files.readAllBytes(fileUpload.filePath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] readBytes(Blob blob) {
        if (blob == null) {
            return null;
        }
        // FIXME: this cast may mean that we should rather return an InputStream via getBinaryStream()?
        try {
            return blob.getBytes(0, (int) blob.length());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
