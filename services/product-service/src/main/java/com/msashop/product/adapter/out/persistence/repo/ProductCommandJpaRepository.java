package com.msashop.product.adapter.out.persistence.repo;

import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.domain.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCommandJpaRepository extends JpaRepository<ProductEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductEntity p set p.stock = p.stock - :qty " +
            "where p.productId = :productId " +
            "and p.stock >= :qty " +
            "and p.useYn = true " +
            "and p.status = :status")
    int decreaseStock(@Param("productId") Long productId,
                      @Param("qty") Integer qty,
                      @Param("status") ProductStatus status);
}
