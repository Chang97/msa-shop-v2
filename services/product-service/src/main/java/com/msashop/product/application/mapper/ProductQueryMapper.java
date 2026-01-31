package com.msashop.product.application.mapper;

import com.msashop.product.application.port.in.model.ProductResult;
import com.msashop.product.application.port.out.model.ProductRow;

public final class ProductQueryMapper {
    private ProductQueryMapper() {}

    public static ProductResult toResult(ProductRow row) {
        return new ProductResult(
                row.getProductId(),
                row.getProductName(),
                row.getPrice(),
                row.getStock(),
                row.getStatus(),
                Boolean.TRUE.equals(row.getUseYn())
        );
    }
}
