package com.deep.ordexaflow.cart.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.catalog.domain.Product;
import com.deep.ordexaflow.users.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(User user, String currency, Instant now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.currency = currency;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public CartItem setProductQuantity(Product product, int quantity, Instant now) {
        CartItem item = findItemByProductId(product.getId()).orElse(null);
        if (item == null) {
            item = new CartItem(this, product, quantity, now);
            items.add(item);
        } else {
            item.changeQuantity(quantity, now);
        }
        updatedAt = now;
        return item;
    }

    public void changeQuantity(UUID itemId, int quantity, Instant now) {
        CartItem item = findItemById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item does not belong to this cart"));
        item.changeQuantity(quantity, now);
        updatedAt = now;
    }

    public boolean removeItem(UUID itemId, Instant now) {
        boolean removed = items.removeIf(item -> item.getId().equals(itemId));
        if (removed) {
            updatedAt = now;
        }
        return removed;
    }

    public void clear(Instant now) {
        if (!items.isEmpty()) {
            items.clear();
            updatedAt = now;
        }
    }

    public Optional<CartItem> findItemById(UUID itemId) {
        return items.stream().filter(item -> item.getId().equals(itemId)).findFirst();
    }

    public Optional<CartItem> findItemByProductId(UUID productId) {
        return items.stream().filter(item -> item.getProduct().getId().equals(productId)).findFirst();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getCurrency() { return currency; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CartItem> getItems() { return List.copyOf(items); }
}
