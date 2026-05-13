package com.msashop.product.adapter.out.persistence.adapter;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.product.adapter.out.persistence.repo.ProductQueryJpaRepository;
import com.msashop.product.application.port.out.LoadProductPort;
import com.msashop.product.application.port.out.model.ProductRow;
import com.msashop.product.domain.model.ProductStatus;
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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_NOT_FOUND, "상품을 찾을 수 없습니다. productId: " + productId));
    }

    @Override
    public List<ProductRow> findAll() {
        return productQueryJpaRepository.findAllByUseYnTrueAndStatusOrderByProductIdAsc(ProductStatus.ON_SALE);
    }
}
