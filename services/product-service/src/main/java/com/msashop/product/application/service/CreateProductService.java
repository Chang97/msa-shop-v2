package com.msashop.product.application.service;

import com.msashop.product.application.port.in.CreateProductUseCase;
import com.msashop.product.application.port.in.model.CreateProductCommand;
import com.msashop.product.application.port.out.SaveProductPort;
import com.msashop.product.domain.model.Product;
import com.msashop.product.domain.model.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {

    private final SaveProductPort saveProductPort;

    @Override
    public Long createProduct(CreateProductCommand command) {
        Product product = new Product(
                null,
                command.productName(),
                command.price(),
                command.stock(),
                ProductStatus.ON_SALE,
                true,
                null,
                null    // 없으면 null
        );
        return saveProductPort.save(product);
    }
}
