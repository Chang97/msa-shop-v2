package com.msashop.product.application.service;

import com.msashop.product.application.port.in.GetProductUseCase;
import com.msashop.product.application.port.in.model.ProductResult;
import com.msashop.product.application.mapper.ProductQueryMapper;
import com.msashop.product.application.port.out.LoadProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GetProductService implements GetProductUseCase {
    private final LoadProductPort loadProductPort;

    @Override
    public ProductResult getProduct(Long productId) {
        return ProductQueryMapper.toResult(loadProductPort.findById(productId));
    }
}
