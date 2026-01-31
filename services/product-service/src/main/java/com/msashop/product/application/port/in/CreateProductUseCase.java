package com.msashop.product.application.port.in;

import com.msashop.product.application.port.in.model.CreateProductCommand;

public interface CreateProductUseCase {
    Long createProduct(CreateProductCommand command);
}
