package com.deep.ordexaflow.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("%s not found: %s".formatted(resourceType, resourceId));
    }
}
