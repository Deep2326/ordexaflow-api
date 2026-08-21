package com.deep.ordexaflow.cart.api;

import java.math.BigDecimal;
import java.util.UUID;

import com.deep.ordexaflow.cart.domain.CartItem;
import com.deep.ordexaflow.catalog.domain.Product;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String sku,
        String name,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal lineTotal,
        int availableQuantity,
        boolean available) {

    static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        int availableQuantity = product.getInventory().getQuantity();
        return new CartItemResponse(
                item.getId(), product.getId(), product.getSku(), product.getName(),
                product.getPrice(), product.getCurrency(), item.getQuantity(),
                product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                availableQuantity,
                product.isActive() && product.getCategory().isActive()
                        && availableQuantity >= item.getQuantity());
    }
}
