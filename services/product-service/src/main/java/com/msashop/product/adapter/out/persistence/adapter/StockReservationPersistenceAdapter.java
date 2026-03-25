package com.msashop.product.adapter.out.persistence.adapter;

import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.common.web.exception.PaymentErrorCode;
import com.msashop.product.adapter.out.persistence.entity.StockReservationEntity;
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
 * 예약 재고는 "가용 stock을 먼저 줄이고, 실패 시 다시 돌려주는 방식"으로 구현한다.
 *
 * 장점:
 * - 추가 reserved_stock 컬럼 없이 현재 stock 컬럼만으로 가용 재고 관리 가능
 * - 주문/결제/재고 경계를 유지하면서도 동시성 제어가 단순하다
 *
 * 주의:
 * - reserve() 중간에 하나라도 실패하면 트랜잭션 롤백으로 전체 예약이 취소된다
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
        Map<Long, Integer> aggregated = items.stream()
                .collect(Collectors.toMap(
                        StockReservationItemPayload::productId,
                        StockReservationItemPayload::quantity,
                        Integer::sum
                ));

        aggregated.forEach((productId, qty) -> {
            int updated = productCommandJpaRepository.decreaseStock(productId, qty, ProductStatus.ON_SALE);
            if (updated == 0) {
                throw new ConflictException(
                        PaymentErrorCode.PAYMENT_STOCK_SHORTAGE,
                        "insufficient stock or concurrent update. productId: " + productId
                );
            }
        });

        List<StockReservationEntity> reservations = aggregated.entrySet().stream()
                .map(entry -> StockReservationEntity.builder()
                        .reservationId(reservationId)
                        .orderId(orderId)
                        .productId(entry.getKey())
                        .quantity(entry.getValue())
                        .status(StockReservationStatus.RESERVED)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .build())
                .toList();

        stockReservationJpaRepository.saveAll(reservations);
    }

    @Override
    public void confirm(String reservationId) {
        List<StockReservationEntity> reservations = stockReservationJpaRepository.findByReservationId(reservationId);
        if (reservations.isEmpty()) {
            throw new ConflictException(
                    CommonErrorCode.COMMON_CONFLICT,
                    "reservation not found. reservationId: " + reservationId
            );
        }

        reservations.forEach(StockReservationEntity::confirm);
    }

    @Override
    public void release(String reservationId) {
        List<StockReservationEntity> reservations = stockReservationJpaRepository.findByReservationId(reservationId);
        if (reservations.isEmpty()) {
            throw new ConflictException(
                    CommonErrorCode.COMMON_CONFLICT,
                    "reservation not found. reservationId: " + reservationId
            );
        }

        for (StockReservationEntity reservation : reservations) {
            if (reservation.getStatus() == StockReservationStatus.RESERVED) {
                // 상태 변경을 먼저 반영해 flush 대상에 올린 뒤 stock 복구 쿼리를 실행한다.
                // increaseStock()는 clearAutomatically=true 이므로 순서가 반대면 RELEASED가 유실될 수 있다.
                reservation.release();
                productCommandJpaRepository.increaseStock(
                        reservation.getProductId(),
                        reservation.getQuantity()
                );
            }
        }
    }
}
