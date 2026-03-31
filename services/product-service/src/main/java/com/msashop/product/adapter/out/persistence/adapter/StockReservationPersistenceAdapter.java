package com.msashop.product.adapter.out.persistence.adapter;

import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.PaymentErrorCode;
import com.msashop.product.adapter.out.persistence.entity.StockReservationEntity;
import com.msashop.product.adapter.out.persistence.entity.StockReservationItemEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.adapter.out.persistence.repo.StockReservationJpaRepository;
import com.msashop.product.application.port.out.StockReservationPort;
import com.msashop.product.domain.model.ProductStatus;
import com.msashop.product.domain.model.StockReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 주문 단위 재고 예약을 저장하고 조회하는 영속성 어댑터.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class StockReservationPersistenceAdapter implements StockReservationPort {

    private final ProductCommandJpaRepository productCommandJpaRepository;
    private final StockReservationJpaRepository stockReservationJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findActiveReservationId(Long orderId) {
        return stockReservationJpaRepository.findFirstByOrderIdAndStatusIn(
                orderId,
                List.of(StockReservationStatus.RESERVED, StockReservationStatus.CONFIRMED)
        ).map(StockReservationEntity::getReservationId);
    }

    @Override
    public void reserve(String reservationId, Long orderId, List<StockReservationItemPayload> items) {
        Map<Long, Integer> aggregated = aggregateQuantities(items);
        decreaseAvailableStock(aggregated);
        saveReservation(reservationId, orderId, aggregated);
    }

    @Override
    public void confirm(String reservationId) {
        StockReservationEntity entity = stockReservationJpaRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.COMMON_CONFLICT,
                        "재고 예약 정보를 찾을 수 없습니다. reservationId: " + reservationId
                ));

        entity.confirm();
    }

    @Override
    public void release(String reservationId) {
        StockReservationEntity entity = stockReservationJpaRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.COMMON_CONFLICT,
                        "재고 예약 정보를 찾을 수 없습니다. reservationId: " + reservationId
                ));

        if (!entity.release()) {
            return;
        }
        restoreAvailableStock(entity.getItems());
    }

    /**
     * 만료 시각이 지난 RESERVED 예약을 EXPIRED로 바꾸고 재고를 복구한다.
     */
    public int expireReservations(Instant now) {
        List<StockReservationEntity> expiredEntities = stockReservationJpaRepository.findAllByStatusAndExpiresAtBefore(
                StockReservationStatus.RESERVED,
                now
        );

        int expiredCount = 0;
        for (StockReservationEntity entity : expiredEntities) {
            if (!entity.expire()) {
                continue;
            }
            restoreAvailableStock(entity.getItems());
            expiredCount++;
        }
        return expiredCount;
    }

    /**
     * 같은 상품이 여러 번 들어오면 상품별 수량으로 합산한다.
     */
    private Map<Long, Integer> aggregateQuantities(List<StockReservationItemPayload> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        StockReservationItemPayload::productId,
                        StockReservationItemPayload::quantity,
                        Integer::sum
                ));
    }

    /**
     * 이 프로젝트의 재고 예약은 가용 재고를 먼저 차감하는 방식으로 동작한다.
     */
    private void decreaseAvailableStock(Map<Long, Integer> aggregated) {
        aggregated.forEach((productId, qty) -> {
            int updated = productCommandJpaRepository.decreaseStock(productId, qty, ProductStatus.ON_SALE);
            if (updated == 0) {
                throw new BusinessException(
                        PaymentErrorCode.PAYMENT_STOCK_SHORTAGE,
                        "재고가 부족하거나 동시성 충돌이 발생했습니다. productId: " + productId
                );
            }
        });
    }

    /**
     * 주문 단위 예약 헤더와 아이템을 저장한다.
     */
    private void saveReservation(String reservationId, Long orderId, Map<Long, Integer> aggregated) {
        StockReservationEntity reservation = StockReservationEntity.of(
                reservationId,
                orderId,
                StockReservationStatus.RESERVED,
                Instant.now().plusSeconds(600)
        );
        aggregated.forEach(reservation::addItem);
        stockReservationJpaRepository.save(reservation);
    }

    /**
     * 예약 아이템 목록을 기준으로 가용 재고를 복구한다.
     */
    private void restoreAvailableStock(List<StockReservationItemEntity> items) {
        for (StockReservationItemEntity item : items) {
            productCommandJpaRepository.increaseStock(
                    item.getProductId(),
                    item.getQuantity()
            );
        }
    }
}
