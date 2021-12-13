package io.quarkiverse.renarde.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
public class MyParamConverters implements ParamConverterProvider {

    public static class DateParamConverter implements ParamConverter<Date> {

        @Override
        public Date fromString(String value) {
            if (StringUtils.isEmpty(value))
                return null;
            if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd").parse(value);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException("Don't know how to deserialise " + value + " as a Date");
        }

        @Override
        public String toString(Date value) {
            // FIXME: is this used?
            return JavaExtensions.internetDateFormat(value);
        }
    }

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType == Date.class)
            return (ParamConverter<T>) new DateParamConverter();
        return null;
    }

}
