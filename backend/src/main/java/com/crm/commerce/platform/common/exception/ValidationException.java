package com.crm.commerce.platform.common.exception;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String field, String message) {
        super("Validation failed");
        this.errors = Map.of(field, message);
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = Collections.unmodifiableMap(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
