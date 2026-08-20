package com.deep.ordexaflow.common.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resourceType, String field, Object value) {
        super("%s already exists with %s: %s".formatted(resourceType, field, value));
    }
}
