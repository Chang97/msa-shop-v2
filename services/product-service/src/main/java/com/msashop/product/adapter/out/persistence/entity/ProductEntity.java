package com.msashop.product.adapter.out.persistence.entity;

import com.msashop.product.domain.model.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ProductEntity extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 120, nullable = false)
    private String productName;

    @PositiveOrZero
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Builder.Default
    @Column(name = "use_yn", nullable = false)
    private Boolean useYn = true;

    public void setStock(Integer stock) {
        this.stock = stock;
    }

}
