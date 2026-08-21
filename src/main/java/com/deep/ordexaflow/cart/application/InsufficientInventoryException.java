package com.deep.ordexaflow.cart.application;

import java.util.UUID;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(UUID productId, long requested, int available) {
        super("Product %s has %d units available; %d requested".formatted(productId, available, requested));
    }
}
