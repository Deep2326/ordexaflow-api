package com.deep.ordexaflow.cart.domain;

import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.catalog.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() {
    }

    CartItem(Cart cart, Product product, int quantity, Instant now) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Cart item quantity must be positive");
        }
        this.id = UUID.randomUUID();
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void changeQuantity(int quantity, Instant now) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Cart item quantity must be positive");
        }
        this.quantity = quantity;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
