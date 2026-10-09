package io.quarkiverse.renarde.transporter.impl;

import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.renarde.transporter.InstanceResolver;
import io.quarkiverse.renarde.transporter.ValueTransformer;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class TransporterUtil {

    public static void serialize(JsonGenerator gen, String name, java.util.Date value,
            Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        java.util.Date transformed = (java.util.Date) transformer.transform(entityType, name, value);
        if (transformed != null) {
            // make sure we serialise to UTC
            String iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(LocalDateTime.ofInstant(transformed.toInstant(), ZoneOffset.UTC));
            gen.writeStringProperty(name, iso);
        }
    }

    public static void serialize(JsonGenerator gen, String name, java.time.Instant value,
            Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        java.time.Instant transformed = (java.time.Instant) transformer.transform(entityType, name, value);
        if (transformed != null) {
            // make sure we serialise to UTC
            String iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(LocalDateTime.ofInstant(transformed, ZoneOffset.UTC));
            gen.writeStringProperty(name, iso);
        }
    }

    public static void serialize(JsonGenerator gen, String name, String value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        String transformed = (String) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeStringProperty(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, long value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        serialize(gen, name, (Long) value, entityType, transformer);
    }

    public static void serialize(JsonGenerator gen, String name, Long value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        Long transformed = (Long) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeNumberProperty(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, int value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        serialize(gen, name, (Integer) value, entityType, transformer);
    }

    public static void serialize(JsonGenerator gen, String name, Integer value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        Integer transformed = (Integer) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeNumberProperty(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, boolean value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        serialize(gen, name, (Boolean) value, entityType, transformer);
    }

    public static void serialize(JsonGenerator gen, String name, Boolean value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        Boolean transformed = (Boolean) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeBooleanProperty(name, transformed);
        }
    }

    public static void serialize(JsonGenerator gen, String name, Blob value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        Blob transformed = (Blob) transformer.transform(entityType, name, value);
        if (transformed != null) {
            try {
                gen.writeBinaryProperty(name, transformed.getBytes(1, (int) transformed.length()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void serialize(JsonGenerator gen, String name, Enum<?> value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        Enum<?> transformed = (Enum<?>) transformer.transform(entityType, name, value);
        if (transformed != null) {
            gen.writeStringProperty(name, transformed.name());
        }
    }

    public static void serialize(JsonGenerator gen, String name, List<? extends PanacheEntity> value,
            Class<? extends PanacheEntity> entityType, ValueTransformer transformer) {
        List<? extends PanacheEntity> transformed = (List<? extends PanacheEntity>) transformer.transform(entityType, name,
                value);
        if (transformed != null) {
            gen.writeArrayPropertyStart(name);
            for (PanacheEntity entity : transformed) {
                writeObjectReference(gen, entity);
            }
            gen.writeEndArray();
        }
    }

    public static void serialize(JsonGenerator gen, String name, PanacheEntity value, Class<? extends PanacheEntity> entityType,
            ValueTransformer transformer) {
        PanacheEntity transformed = (PanacheEntity) transformer.transform(entityType, name, value);
        if (transformed != null) {
            writeObjectReference(gen, name, transformed);
        }
    }

    public static java.util.Date deserializeDate(JsonParser p) {
        String iso = deserializeText(p);
        if (iso == null)
            return null;
        LocalDateTime localDateTime = LocalDateTime.parse(iso);
        // we serialise to UTC
        return java.util.Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    public static java.time.Instant deserializeInstant(JsonParser p) {
        String iso = deserializeText(p);
        if (iso == null)
            return null;
        LocalDateTime localDateTime = LocalDateTime.parse(iso);
        // we serialise to UTC
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    public static long deserializeLong(JsonParser p) {
        return p.nextLongValue(0);
    }

    public static Long deserializeBoxedLong(JsonParser p) {
        return p.nextLongValue(0);
    }

    public static int deserializeInt(JsonParser p) {
        return p.nextIntValue(0);
    }

    public static Integer deserializeBoxedInteger(JsonParser p) {
        return p.nextIntValue(0);
    }

    public static boolean deserializeBoolean(JsonParser p) {
        return p.nextBooleanValue();
    }

    public static Boolean deserializeBoxedBoolean(JsonParser p) {
        return p.nextBooleanValue();
    }

    public static String deserializeText(JsonParser p) {
        return p.nextStringValue();
    }

    public static <T extends Enum<T>> T deserializeEnum(JsonParser p, Class<T> enumClass) {
        String val = deserializeText(p);
        return val != null ? Enum.valueOf(enumClass, val) : null;
    }

    public static Blob deserializeBlob(JsonParser p) {
        p.nextToken();
        byte[] value = p.getBinaryValue();
        return value != null ? Panache.getSession().getLobHelper().createBlob(value) : null;
    }

    public static List<? extends PanacheEntity> deserializeMultiRelation(JsonParser p, InstanceResolver resolver) {
        if (p.nextToken() != JsonToken.START_ARRAY) {
            throw new AssertionError("Expected start of array");
        }
        List<PanacheEntity> ret = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            ret.add(resolver.resolveReference(p, true));
        }
        return ret;
    }

    public static PanacheEntity deserializeRelation(JsonParser p, InstanceResolver resolver) {
        return resolver.resolveReference(p);
    }

    private static void writeObjectReference(JsonGenerator gen, String name, PanacheEntity entity) {
        if (entity == null) {
            gen.writeNullProperty(name);
        } else {
            gen.writeObjectPropertyStart(name);
            gen.writeNumberProperty("id", entity.id);
            gen.writeStringProperty("_type", entity.getClass().getName());
            gen.writeEndObject();
        }
    }

    private static void writeObjectReference(JsonGenerator gen, PanacheEntity entity) {
        if (entity == null) {
            gen.writeNull();
        } else {
            gen.writeStartObject();
            gen.writeNumberProperty("id", entity.id);
            gen.writeStringProperty("_type", entity.getClass().getName());
            gen.writeEndObject();
        }
    }
}
