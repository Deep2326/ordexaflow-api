package com.deep.ordexaflow.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpdateRequest(@NotNull @Min(0) Integer quantity) {
}
