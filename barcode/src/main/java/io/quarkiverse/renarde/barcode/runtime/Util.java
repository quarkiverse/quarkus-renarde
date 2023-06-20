package io.quarkiverse.renarde.barcode.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.qute.Expression;
import io.quarkus.qute.SectionHelperFactory.BlockInfo;
import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import io.quarkus.qute.TemplateException;

public class Util {

    public static Map<String, Expression> collectExpressions(SectionInitContext context, String... names) {
        Map<String, Expression> ret = new HashMap<>();
        Set<String> expectedParameters = new HashSet<>(context.getParameters().keySet());
        for (String name : names) {
            Expression value = context.getExpression(name);
            if (value != null) {
                ret.put(name, value);
            }
            expectedParameters.remove(name);
        }
        if (!expectedParameters.isEmpty()) {
            throw new TemplateException("Unexpected parameters to template: " + expectedParameters + " (we only know about "
                    + Arrays.toString(names) + ")");
        }
        return ret;
    }

    public static void declareBlock(BlockInfo block, String... names) {
        for (String name : names) {
            String value = block.getParameter(name);
            if (value != null) {
                block.addExpression(name, value);
            }
        }
    }

    /**
     * This is for runtime checks
     */
    public static <T> T typecheckValue(Map<String, Object> values, String name, Class<? extends T> type) {
        return typecheckValue(values, name, type, null);
    }

    /**
     * This is for runtime checks
     */
    public static <T> T typecheckValue(Map<String, Object> values, String name, Class<? extends T> type, T defaultValue) {
        Object valueObject = values.get(name);
        if (valueObject == null) {
            return defaultValue;
        }
        if (!type.isAssignableFrom(valueObject.getClass()))
            throw new TemplateException("Invalid " + name + " parameter: " + valueObject + " should be of type " + type
                    + " but is of type " + valueObject.getClass());
        return (T) valueObject;
    }

    public static void requireParameter(SectionInitContext context, String name) {
        if (context.getParameter(name) == null) {
            throw new IllegalStateException("Missing parameter: " + name);
        }
    }
}
