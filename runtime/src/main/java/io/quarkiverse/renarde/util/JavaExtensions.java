package io.quarkiverse.renarde.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import io.quarkus.arc.Arc;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class JavaExtensions {

    public static String prepend(String string, Object value) {
        return value + string;
    }

    public static String append(String string, Object value) {
        return string + value;
    }

    public static String htmlNormalised(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(date);
    }

    public static String format(Date date) {
        // FIXME: L10N
        return new SimpleDateFormat("dd/MM/yyyy").format(date);
    }

    public static String internetDateFormat(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public static boolean isFuture(Date date) {
        return date.after(new Date());
    }

    public static String since(Date date) {
        LocalDateTime t1 = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        LocalDateTime t2 = LocalDateTime.now();
        Period period = Period.between(t1.toLocalDate(), t2.toLocalDate());
        Duration duration = Duration.between(t1, t2);

        if (period.getYears() > 1)
            return period.getYears() + " years ago";
        if (period.getYears() == 1)
            return period.getYears() + " year ago";
        if (period.getMonths() > 1)
            return period.getMonths() + " months ago";
        if (period.getMonths() == 1)
            return period.getMonths() + " month ago";
        if (period.getDays() > 1)
            return period.getDays() + " days ago";
        if (period.getDays() == 1)
            return period.getDays() + " day ago";
        if (duration.toHours() > 1)
            return duration.toHours() + " hours ago";
        if (duration.toHours() == 1)
            return duration.toHours() + " hour ago";
        if (duration.toMinutes() > 1)
            return duration.toMinutes() + " minutes ago";
        if (duration.toMinutes() == 1)
            return duration.toMinutes() + " minute ago";
        return "moments ago";
    }

    public static String gravatarHash(String str) {
        if (str == null)
            return null;
        return md5(str.trim().toLowerCase());
    }

    public static String md5(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (int i = 0; i < digest.length; ++i) {
                sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // workaround a boxing bug?
    public static int minus(int a, int b) {
        return a - b;
    }

    public static boolean instanceOf(Object val, String type) {
        if (val == null)
            return false;
        return val.getClass().getName().equals(type);
    }

    public static String capitalised(String val) {
        if (val == null || val.isEmpty())
            return val;
        // FIXME: doesn't respect codepoints, perhaps not even the Turkish capital i?
        return val.substring(0, 1).toUpperCase() + val.substring(1);
    }

    @TemplateExtension(namespace = "flash", matchName = "*")
    static Object flash(String value) {
        return Arc.container().instance(Flash.class).get().get(value);
    }
}
