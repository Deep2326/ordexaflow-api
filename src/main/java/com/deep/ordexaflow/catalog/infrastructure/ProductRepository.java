package com.deep.ordexaflow.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.catalog.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = {"category", "inventory"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "inventory"})
    Optional<Product> findOneById(UUID id);

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
}
