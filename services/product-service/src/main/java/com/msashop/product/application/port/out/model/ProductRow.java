package com.msashop.product.application.port.out.model;

import java.math.BigDecimal;
import com.msashop.product.domain.model.ProductStatus;

public interface ProductRow {
    Long getProductId();
    String getProductName();
    BigDecimal getPrice();
    int getStock();
    ProductStatus getStatus();
    Boolean getUseYn();
}
