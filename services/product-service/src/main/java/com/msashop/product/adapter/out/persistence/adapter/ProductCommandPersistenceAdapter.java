package com.msashop.product.adapter.out.persistence.adapter;

import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.application.port.out.SaveProductPort;
import com.msashop.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCommandPersistenceAdapter implements SaveProductPort {
    private final ProductCommandJpaRepository productCommandJpaRepository;

    @Override
    public Long save(Product product) {
        ProductEntity entity = ProductEntity.builder()
                .productId(null) // 새로 생성
                .productName(product.getProductName())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .useYn(product.isUseYn())
                .build();

        return productCommandJpaRepository.save(entity).getProductId();
    }
}
