package io.quarkiverse.renarde.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path.Node;
import jakarta.validation.Validator;

@Named("validation")
@RequestScoped
public class Validation {
    @Inject
    Validator validator;
    @Inject
    Flash flash;

    private List<Error> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void keep() {
        for (Error error : errors) {
            flash.flash("error." + error.field, error.message);
        }
    }

    public void required(String field, Object value) {
        if (value == null || (value instanceof String && ((String) value).isEmpty()))
            addError(field, "Required");
    }

    public void addError(String field, String message) {
        errors.add(new Error(field, message));
    }

    // Called from ifError.html
    public boolean hasError(String field) {
        for (Error error : errors) {
            if (error.field.equals(field))
                return true;
        }
        return false;
    }

    // Called from error.html
    public String getError(String field) {
        for (Error error : errors) {
            if (error.field.equals(field))
                return error.message;
        }
        return null;
    }

    public static class Error {

        public final String field;
        public final String message;

        public Error(String field, String message) {
            this.field = field;
            this.message = message;
        }

        @Override
        public String toString() {
            return "[Error field=" + field + ", message=" + message + "]";
        }
    }

    public void minSize(String field, String value, int size) {
        if (value == null || value.length() < size)
            addError(field, "Must be at least " + size + " characters long");
    }

    public void maxSize(String field, String value, int size) {
        if (value == null || value.length() > size)
            addError(field, "Must be at most " + size + " characters long");
    }

    public void equals(String field, Object a, Object b) {
        if (!Objects.equals(a, b))
            addError(field, "Must be equal");
    }

    public void future(String field, Date date) {
        if (!date.after(new Date()))
            addError(field, "Must be in the future");
    }

    public void addErrors(Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> violation : violations) {
            Iterator<Node> iterator = violation.getPropertyPath().iterator();
            String lastNode = null;
            while (iterator.hasNext()) {
                lastNode = iterator.next().getName();
            }
            addError(lastNode, violation.getMessage());
        }
    }

    public void loadErrorsFromFlash() {
        for (Entry<String, Object> entry : flash.values().entrySet()) {
            if (entry.getKey().startsWith("error.")) {
                String field = entry.getKey().substring(6);
                addError(field, (String) entry.getValue());
            }
        }
    }
}
