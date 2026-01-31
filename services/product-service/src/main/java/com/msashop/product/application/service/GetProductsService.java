package com.msashop.product.application.service;

import com.msashop.product.application.mapper.ProductQueryMapper;
import com.msashop.product.application.port.in.GetProductsUseCase;
import com.msashop.product.application.port.in.model.ProductResult;
import com.msashop.product.application.port.out.LoadProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductsService implements GetProductsUseCase {
    private final LoadProductPort loadProductPort;

    @Override
    public List<ProductResult> getProducts() {
        return loadProductPort.findAll().stream()
                .map(ProductQueryMapper::toResult)
                .toList();
    }
}

