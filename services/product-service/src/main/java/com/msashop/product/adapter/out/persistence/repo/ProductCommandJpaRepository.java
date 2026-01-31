package com.msashop.product.adapter.out.persistence.repo;

import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCommandJpaRepository extends JpaRepository<ProductEntity, Long> {
}
