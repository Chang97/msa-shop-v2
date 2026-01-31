package com.msashop.product.application.port.in;

import com.msashop.product.application.port.in.model.ProductResult;

public interface GetProductUseCase {
    ProductResult getProduct(Long productId);
}
