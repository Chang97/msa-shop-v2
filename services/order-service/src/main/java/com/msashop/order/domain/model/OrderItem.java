package com.msashop.order.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {
    private final Long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineAmount;

    public OrderItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.productName = Objects.requireNonNull(productName, "productName");
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = quantity;
        this.unitPrice = requireNonNegative(unitPrice, "unitPrice");
        this.lineAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " 값은 0 이상이어야 합니다.");
        }
        return value;
    }
}
