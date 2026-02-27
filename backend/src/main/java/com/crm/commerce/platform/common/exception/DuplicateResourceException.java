package com.crm.commerce.platform.common.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value));
    }
}
