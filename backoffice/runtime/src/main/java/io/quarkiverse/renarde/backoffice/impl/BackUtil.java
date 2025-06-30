package io.quarkiverse.renarde.backoffice.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
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

import jakarta.ws.rs.core.Response;

import org.hibernate.engine.spi.ManagedEntity;
import org.jboss.resteasy.reactive.common.util.types.TypeSignatureParser;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.renarde.jpa.NamedBlob;
import io.quarkiverse.renarde.util.FileUtils;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.qute.TemplateData;

@TemplateData
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

    public static java.sql.Timestamp sqlTimestampField(String value) {
        if (!isSet(value))
            return null;
        // FIXME: support nanoseconds later
        return java.sql.Timestamp.valueOf(localDateTimeField(value));
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

    public static Integer integerWrapperField(String value) {
        return isSet(value) ? Integer.valueOf(value) : null;
    }

    public static Long longWrapperField(String value) {
        return isSet(value) ? Long.valueOf(value) : null;
    }

    public static Double doubleWrapperField(String value) {
        return isSet(value) ? Double.valueOf(value) : null;
    }

    public static long longField(String value) {
        return isSet(value) ? Long.valueOf(value) : 0;
    }

    public static Float floatWrapperField(String value) {
        return isSet(value) ? Float.valueOf(value) : 0;
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

    public static <T> T jsonField(String typeSignature, String value) {
        if (!isSet(value))
            return null;
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Type type = TypeSignatureParser.parse(typeSignature);
        try {
            return mapper.readValue(value, mapper.constructType(type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(Object value) {
        if (value == null) {
            return "";
        }
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        try {
            StringWriter w = new StringWriter();
            mapper.writeValue(w, value);
            return w.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> enumPossibleValues(Enum<?>[] values) {
        Map<String, String> ret = new TreeMap<>();
        for (Enum<?> value : values) {
            ret.put(value.name(), value.name());
        }
        return ret;
    }

    public static Map<String, String> entityPossibleValues(List<PanacheEntityBase> list) {
        Map<String, String> ret = new TreeMap<>();
        for (PanacheEntityBase entity : list) {
            ret.put(String.valueOf(id(entity)), entity.toString());
        }
        return ret;
    }

    private static Object id(PanacheEntityBase entity) {
        if (entity instanceof PanacheEntity) {
            return ((PanacheEntity) entity).id;
        }
        if (entity instanceof ManagedEntity) {
            return ((ManagedEntity) entity).$$_hibernate_getEntityEntry().getId();
        }
        throw new RuntimeException("Don't know how to load @Id from entity: " + entity);
    }

    public static List<String> entityCurrentValues(List<PanacheEntityBase> list) {
        List<String> ret = new ArrayList<>(list.size());
        for (PanacheEntityBase entity : list) {
            ret.add(String.valueOf(id(entity)));
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
            return Panache.getSession().getLobHelper().createBlob(Files.readAllBytes(fileUpload.filePath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static NamedBlob namedBlobField(FileUpload fileUpload) {
        if (!isSet(fileUpload))
            return null;
        try {
            NamedBlob blob = new NamedBlob();
            blob.name = fileUpload.fileName();
            blob.mimeType = fileUpload.contentType();
            byte[] bytes = Files.readAllBytes(fileUpload.filePath());
            if (blob.mimeType == null || blob.mimeType.isEmpty())
                blob.mimeType = FileUtils.getMimeType(blob.name, bytes);
            blob.contents = Panache.getSession().getLobHelper().createBlob(bytes);
            return blob;
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
            return blob.getBytes(1, (int) blob.length());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Response binaryResponse(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return Response.noContent().build();
        String mime = FileUtils.getMimeType("", bytes);
        return Response.ok(bytes, mime).build();
    }

    public static Response binaryResponse(Blob blob) {
        if (blob == null)
            return Response.noContent().build();
        return binaryResponse(readBytes(blob));
    }

    public static Response binaryResponse(NamedBlob namedBlob) {
        if (namedBlob == null)
            return Response.noContent().build();
        return Response.ok(readBytes(namedBlob.contents), namedBlob.mimeType)
                .header("Content-Disposition", "attachment; filename=\"" + namedBlob.name + "\"")
                .build();
    }
}
