package com.deep.ordexaflow.cart.api;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull UUID productId,
        @Min(1) int quantity) {
}
