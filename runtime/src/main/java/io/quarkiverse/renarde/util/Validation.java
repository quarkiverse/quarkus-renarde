package io.quarkiverse.renarde.util;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

    private Map<String, Error> errors = new TreeMap<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void keep() {
        for (Error error : errors.values()) {
            flash.flash("error." + error.field, error.getMessage("\f"));
        }
    }

    public void required(String field, Object value) {
        if (value == null || (value instanceof String && ((String) value).isEmpty()))
            addError(field, "Required.");
    }

    public void addError(String field, String message) {
        Error error = errors.get(field);
        if (error == null) {
            error = new Error(field, message);
            errors.put(field, error);
        } else {
            error.addMessage(message);
        }
    }

    // Called from ifError.html
    public boolean hasError(String field) {
        return errors.containsKey(field);
    }

    // Called from error.html
    public String getError(String field) {
        Error error = errors.get(field);
        return error != null ? error.getMessage() : null;
    }

    public static class Error {

        public final String field;
        public final Set<String> messages = new TreeSet<>();

        public Error(String field, String message) {
            this.field = field;
            this.messages.add(message);
        }

        public String getMessage() {
            return getMessage(" ");
        }

        public String getMessage(String delimiter) {
            return String.join(delimiter, messages);
        }

        public void addMessage(String message) {
            messages.add(message);
        }

        @Override
        public String toString() {
            return "[Error field=" + field + ", message=" + messages + "]";
        }
    }

    public void minSize(String field, String value, int size) {
        if (value == null || value.length() < size)
            addError(field, "Must be at least " + size + " characters long.");
    }

    public void maxSize(String field, String value, int size) {
        if (value == null || value.length() > size)
            addError(field, "Must be at most " + size + " characters long.");
    }

    public void equals(String field, Object a, Object b) {
        if (!Objects.equals(a, b))
            addError(field, "Must be equal.");
    }

    public void future(String field, Date date) {
        if (!date.after(new Date()))
            addError(field, "Must be in the future.");
    }

    public void addErrors(Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> violation : violations) {
            Iterator<Node> iterator = violation.getPropertyPath().iterator();
            String lastNode = null;
            while (iterator.hasNext()) {
                lastNode = iterator.next().getName();
            }
            addError(lastNode, adaptBeanValidationMessage(violation.getMessage()));
        }
    }

    private String adaptBeanValidationMessage(String message) {
        // FIXME: this is not I18N but a good start
        switch (message) {
            case "must not be blank":
            case "must not be null":
            case "must not be empty":
                message = "Required";
        }
        return JavaExtensions.capitalised(message) + ".";
    }

    public void loadErrorsFromFlash() {
        for (Entry<String, Object> entry : flash.values().entrySet()) {
            if (entry.getKey().startsWith("error.")) {
                String field = entry.getKey().substring(6);
                String value = (String) entry.getValue();
                for (String error : value.split("\f")) {
                    addError(field, error);
                }
            }
        }
    }
}
