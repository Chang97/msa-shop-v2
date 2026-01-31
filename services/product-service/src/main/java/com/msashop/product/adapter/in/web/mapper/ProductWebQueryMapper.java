package com.msashop.product.adapter.in.web.mapper;

import com.msashop.product.adapter.in.web.dto.ProductResponse;
import com.msashop.product.application.port.in.model.ProductResult;

public final class ProductWebQueryMapper {
    private ProductWebQueryMapper() {}

    public static ProductResponse toResponse(ProductResult result) {
        return new ProductResponse(
                result.productId(),
                result.productName(),
                result.price(),
                result.stock(),
                result.status(),
                result.useYn()
        );
    }
}
