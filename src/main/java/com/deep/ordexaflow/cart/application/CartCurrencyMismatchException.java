package com.deep.ordexaflow.cart.application;

public class CartCurrencyMismatchException extends RuntimeException {
    public CartCurrencyMismatchException(String expected, String actual) {
        super("Cart currency is %s but product currency is %s".formatted(expected, actual));
    }
}
