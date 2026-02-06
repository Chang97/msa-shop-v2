package com.msashop.product.application.service;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.application.port.in.DecreaseStockUseCase;
import com.msashop.product.application.port.in.model.DecreaseStockCommand;
import com.msashop.product.domain.model.ProductStatus;
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
                    .orElseThrow(() -> new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "product not found. productId: " + productId));

            if (Boolean.FALSE.equals(entity.getUseYn())) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "product disabled. productId: " + productId);
            }
            if (entity.getStatus() != ProductStatus.ON_SALE) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "product not on sale. productId: " + productId);
            }
            if (entity.getStock() == null || entity.getStock() < qty) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "insufficient stock. productId: " + productId);
            }

            entity.setStock(entity.getStock() - qty);
        });
    }

    private Map<Long, Integer> aggregate(List<DecreaseStockCommand> commands) {
        Map<Long, Integer> aggregated = new HashMap<>();
        commands.forEach(cmd -> {
            if (cmd == null || cmd.productId() == null) {
                throw new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "product not found. productId: null");
            }
            if (cmd.quantity() <= 0) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "quantity must be positive. productId: " + cmd.productId());
            }
            aggregated.merge(cmd.productId(), cmd.quantity(), Integer::sum);
        });
        return aggregated;
    }
}

