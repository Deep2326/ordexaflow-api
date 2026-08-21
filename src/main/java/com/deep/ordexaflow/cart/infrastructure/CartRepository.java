package com.deep.ordexaflow.cart.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.cart.domain.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "items.product.inventory"})
    @Query("select distinct cart from Cart cart where cart.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") UUID userId);
}
