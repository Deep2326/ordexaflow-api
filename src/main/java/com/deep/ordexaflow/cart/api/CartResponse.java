package com.deep.ordexaflow.cart.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.deep.ordexaflow.cart.domain.Cart;

public record CartResponse(
        UUID id,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal subtotal,
        String currency,
        Instant updatedAt) {

    public static CartResponse empty(String currency) {
        return new CartResponse(null, List.of(), 0, new BigDecimal("0.00"), currency, null);
    }

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new CartResponse(cart.getId(), items, totalItems, subtotal, cart.getCurrency(), cart.getUpdatedAt());
    }
}
