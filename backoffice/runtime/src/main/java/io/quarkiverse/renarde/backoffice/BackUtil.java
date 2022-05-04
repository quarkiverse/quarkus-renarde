package io.quarkiverse.renarde.backoffice;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

public class BackUtil {

    public static Date dateField(String value) {
        if (!isSet(value))
            return null;
        // seconds part is optional
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(value);
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(value);
            } catch (ParseException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public static boolean booleanField(String value) {
        return value != null && value.equals("on");
    }

    public static long longField(String value) {
        return isSet(value) ? Long.valueOf(value) : 0;
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

    public static Map<String, String> enumValues(Enum<?>[] values) {
        Map<String, String> ret = new TreeMap<>();
        for (Enum<?> value : values) {
            ret.put(value.name(), value.name());
        }
        return ret;
    }

    public static Map<String, String> entityValues(List<PanacheEntity> list) {
        Map<String, String> ret = new TreeMap<>();
        for (PanacheEntity entity : list) {
            ret.put(String.valueOf(entity.id), entity.toString());
        }
        return ret;
    }

    public static boolean isSet(String value) {
        return value != null && !value.isEmpty();
    }
}
