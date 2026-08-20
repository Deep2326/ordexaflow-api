package com.deep.ordexaflow.catalog.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventories")
public class Inventory {
    @Id
    @Column(name = "product_id")
    private UUID productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
    }

    Inventory(Product product, int quantity, Instant now) {
        this.product = product;
        this.quantity = quantity;
        this.updatedAt = now;
    }

    public void setQuantity(int quantity, Instant now) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Inventory quantity must not be negative");
        }
        this.quantity = quantity;
        this.updatedAt = now;
    }

    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
