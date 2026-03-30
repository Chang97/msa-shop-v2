package com.msashop.product.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Product {

    private final Long productId;
    private String productName;
    private BigDecimal price;
    private int stock;
    private ProductStatus status;
    private boolean useYn;

    private Instant updatedAt;
    private Long updatedBy;

    public Product(
            Long productId,
            String productName,
            BigDecimal price,
            int stock,
            ProductStatus status,
            boolean useYn,
            Instant updatedAt,
            Long updatedBy
    ) {
        this.productId = productId;
        this.productName = Objects.requireNonNull(productName, "productName");
        this.price = requireNonNegative(price, "price");
        this.stock = requireNonNegative(stock, "stock");
        this.status = Objects.requireNonNullElse(status, ProductStatus.ON_SALE);
        this.useYn = useYn;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public boolean isUseYn() {
        return useYn;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void updateBasicInfo(String productName, BigDecimal price, Boolean useYn) {
        if (productName != null) {
            this.productName = productName;
        }
        if (price != null) {
            this.price = requireNonNegative(price, "price");
        }
        if (useYn != null) {
            this.useYn = useYn;
        }
    }

    public void changeStatus(ProductStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void increaseStock(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        this.stock = requireNonNegative((long) this.stock + amount, "stock");
    }

    public void decreaseStock(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        this.stock = requireNonNegative(this.stock - amount, "stock");
    }

    private BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " 값은 0 이상이어야 합니다.");
        }
        return value;
    }

    private int requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " 값은 0 이상이어야 합니다.");
        }
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field + " 값이 너무 큽니다.");
        }
        return (int) value;
    }
}
