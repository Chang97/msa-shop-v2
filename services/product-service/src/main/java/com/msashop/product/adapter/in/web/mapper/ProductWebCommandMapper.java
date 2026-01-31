package com.msashop.product.adapter.in.web.mapper;

import com.msashop.product.adapter.in.web.dto.CreateProductRequest;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.application.port.in.model.CreateProductCommand;

public final class ProductWebCommandMapper {
    private ProductWebCommandMapper() {}

    public static CreateProductCommand toCommand(CreateProductRequest req) {
        return new CreateProductCommand(
                req.productName(),
                req.price(),
                req.stock()
        );
    }
}
