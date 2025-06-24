package io.quarkiverse.renarde.transporter.impl;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import io.quarkiverse.renarde.transporter.InstanceResolver;
import io.quarkiverse.renarde.transporter.ValueTransformer;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

public class TransporterUtil {

    public static void serialize(JsonGenerator gen, String name, java.util.Date value,
            Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        java.util.Date transformed = (java.util.Date) transformer.transform(entityType, name, value);
        if (transformed != null) {
            // make sure we serialise to UTC
            String iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(LocalDateTime.ofInstant(transformed.toInstant(), ZoneOffset.UTC));
            gen.writeStringField(name, iso);
        }
    }

    public static void serialize(JsonGenerator gen, String name, String value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        String transformed = (String) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeStringField(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, long value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        serialize(gen, name, (Long) value, entityType, transformer);
    }

    public static void serialize(JsonGenerator gen, String name, Long value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        Long transformed = (Long) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeNumberField(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, int value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        serialize(gen, name, (Integer) value, entityType, transformer);
    }

    public static void serialize(JsonGenerator gen, String name, Integer value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        Integer transformed = (Integer) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeNumberField(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, boolean value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        serialize(gen, name, (Boolean) value, entityType, transformer);
    }

    public static void serialize(JsonGenerator gen, String name, Boolean value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        Boolean transformed = (Boolean) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeBooleanField(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, Blob value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        Blob transformed = (Blob) transformer.transform(entityType, name, value);
        if (transformed != null) {
            try {
                gen.writeBinaryField(name, transformed.getBytes(1, (int) transformed.length()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void serialize(JsonGenerator gen, String name, Enum<?> value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        Enum<?> transformed = (Enum<?>) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeStringField(name, transformed.name());
        }
    }

    public static void serialize(JsonGenerator gen, String name, List<? extends PanacheEntity> value,
            Class<? extends PanacheEntity> entityType, ValueTransformer transformer) throws IOException {
        List<? extends PanacheEntity> transformed = (List<? extends PanacheEntity>) transformer.transform(entityType, name,
                value);
        if (transformed != null) {
            gen.writeArrayFieldStart(name);
            for (PanacheEntity entity : transformed) {
                writeObjectReference(gen, entity);
            }
            gen.writeEndArray();
        }
    }

    public static void serialize(JsonGenerator gen, String name, PanacheEntity value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) throws IOException {
        PanacheEntity transformed = (PanacheEntity) transformer.transform(entityType, name, value);
        if (transformed != null) {
            writeObjectReference(gen, name, transformed);
        }
    }

    public static java.util.Date deserializeDate(JsonParser p) throws IOException {
        String iso = deserializeText(p);
        if (iso == null)
            return null;
        LocalDateTime localDateTime = LocalDateTime.parse(iso);
        // we serialise to UTC
        return java.util.Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    public static long deserializeLong(JsonParser p) throws IOException {
        return p.nextLongValue(0);
    }

    public static Long deserializeBoxedLong(JsonParser p) throws IOException {
        return p.nextLongValue(0);
    }

    public static int deserializeInt(JsonParser p) throws IOException {
        return p.nextIntValue(0);
    }

    public static Integer deserializeBoxedInteger(JsonParser p) throws IOException {
        return p.nextIntValue(0);
    }

    public static boolean deserializeBoolean(JsonParser p) throws IOException {
        return p.nextBooleanValue();
    }

    public static Boolean deserializeBoxedBoolean(JsonParser p) throws IOException {
        return p.nextBooleanValue();
    }

    public static String deserializeText(JsonParser p) throws IOException {
        return p.nextTextValue();
    }

    public static <T extends Enum<T>> T deserializeEnum(JsonParser p, Class<T> enumClass) throws IOException {
        String val = deserializeText(p);
        return val != null ? Enum.valueOf(enumClass, val) : null;
    }

    public static Blob deserializeBlob(JsonParser p) throws IOException {
        p.nextToken();
        byte[] value = p.getBinaryValue();
        return value != null ? Panache.getSession().getLobHelper().createBlob(value) : null;
    }

    public static List<? extends PanacheEntity> deserializeMultiRelation(JsonParser p, InstanceResolver resolver)
            throws IOException {
        if (p.nextToken() != JsonToken.START_ARRAY) {
            throw new AssertionError("Expected start of array");
        }
        List<PanacheEntity> ret = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            ret.add(resolver.resolveReference(p, true));
        }
        return ret;
    }

    public static PanacheEntity deserializeRelation(JsonParser p, InstanceResolver resolver) throws IOException {
        return resolver.resolveReference(p);
    }

    private static void writeObjectReference(JsonGenerator gen, String name, PanacheEntity entity) throws IOException {
        if (entity == null) {
            gen.writeNullField(name);
        } else {
            gen.writeObjectFieldStart(name);
            gen.writeNumberField("id", entity.id);
            gen.writeStringField("_type", entity.getClass().getName());
            gen.writeEndObject();
        }
    }

    private static void writeObjectReference(JsonGenerator gen, PanacheEntity entity) throws IOException {
        if (entity == null) {
            gen.writeNull();
        } else {
            gen.writeStartObject();
            gen.writeNumberField("id", entity.id);
            gen.writeStringField("_type", entity.getClass().getName());
            gen.writeEndObject();
        }
    }
}
