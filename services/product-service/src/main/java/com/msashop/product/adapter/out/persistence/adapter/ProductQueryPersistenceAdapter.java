package com.msashop.product.adapter.out.persistence.adapter;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.product.adapter.out.persistence.repo.ProductQueryJpaRepository;
import com.msashop.product.application.port.out.LoadProductPort;
import com.msashop.product.application.port.out.model.ProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductQueryPersistenceAdapter implements LoadProductPort {
    private final ProductQueryJpaRepository productQueryJpaRepository;

    @Override
    public ProductRow findById(Long productId) {
        return productQueryJpaRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "product not found. productId: " + productId));
    }

    @Override
    public List<ProductRow> findAll() {
        return productQueryJpaRepository.findAllBy();
    }
}
