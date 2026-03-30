package com.msashop.product.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.application.port.in.DecreaseStockUseCase;
import com.msashop.product.application.port.in.model.DecreaseStockCommand;
import com.msashop.product.domain.model.ProductStatus;
import com.msashop.common.web.exception.PaymentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DecreaseStockService implements DecreaseStockUseCase {

    private final ProductCommandJpaRepository productCommandJpaRepository;

    @Override
    public void decreaseStocks(List<DecreaseStockCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        Map<Long, Integer> aggregated = aggregate(commands);

        aggregated.forEach((productId, qty) -> {
            ProductEntity entity = productCommandJpaRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_NOT_FOUND, "상품을 찾을 수 없습니다. productId: " + productId));

            if (Boolean.FALSE.equals(entity.getUseYn())) {
                throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, "비활성화된 상품입니다. productId: " + productId);
            }
            if (entity.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, "판매 중인 상품이 아닙니다. productId: " + productId);
            }
            if (entity.getStock() == null || entity.getStock() < qty) {
                throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, "재고가 부족합니다. productId: " + productId);
            }

            int updated = productCommandJpaRepository.decreaseStock(productId, qty, ProductStatus.ON_SALE);
            if (updated == 0) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_STOCK_SHORTAGE,
                        "재고가 부족하거나 동시성 충돌이 발생했습니다. productId: " + productId);
            }
        });
    }

    private Map<Long, Integer> aggregate(List<DecreaseStockCommand> commands) {
        Map<Long, Integer> aggregated = new HashMap<>();
        commands.forEach(cmd -> {
            if (cmd == null || cmd.productId() == null) {
                throw new BusinessException(CommonErrorCode.COMMON_NOT_FOUND, "상품 식별자가 올바르지 않습니다. productId: null");
            }
            if (cmd.quantity() <= 0) {
                throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, "수량은 1 이상이어야 합니다. productId: " + cmd.productId());
            }
            aggregated.merge(cmd.productId(), cmd.quantity(), Integer::sum);
        });
        return aggregated;
    }
}
