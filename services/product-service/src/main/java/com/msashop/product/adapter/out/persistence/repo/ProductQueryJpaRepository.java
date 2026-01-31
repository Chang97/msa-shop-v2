package com.msashop.product.adapter.out.persistence.repo;

import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.application.port.out.model.ProductRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductQueryJpaRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductRow> findByProductId(Long productId);
    List<ProductRow> findAllBy();
}
