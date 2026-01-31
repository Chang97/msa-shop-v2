package com.msashop.product.application.port.in;

import com.msashop.product.application.port.in.model.ProductResult;

import java.util.List;

public interface GetProductsUseCase {
    List<ProductResult> getProducts();
}

