package com.msashop.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {

    private final Long orderId;
    private final String orderNumber;
    private final Long userId;
    private OrderStatus status;
    private final String currency;

    private final BigDecimal subtotalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal shippingFee;
    private final BigDecimal totalAmount;

    private final String receiverName;
    private final String receiverPhone;
    private final String shippingPostcode;
    private final String shippingAddress1;
    private final String shippingAddress2;
    private final String memo;

    private final List<OrderItem> items;

    private final Instant createdAt;
    private final Instant updatedAt;

    private Order(
            Long orderId,
            String orderNumber,
            Long userId,
            OrderStatus status,
            String currency,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String receiverName,
            String receiverPhone,
            String shippingPostcode,
            String shippingAddress1,
            String shippingAddress2,
            String memo,
            List<OrderItem> items,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.orderId = orderId;
        this.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = Objects.requireNonNull(status, "status");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.subtotalAmount = requireNonNegative(subtotalAmount, "subtotalAmount");
        this.discountAmount = requireNonNegative(discountAmount, "discountAmount");
        this.shippingFee = requireNonNegative(shippingFee, "shippingFee");
        this.totalAmount = requireNonNegative(totalAmount, "totalAmount");
        this.receiverName = Objects.requireNonNull(receiverName, "receiverName");
        this.receiverPhone = Objects.requireNonNull(receiverPhone, "receiverPhone");
        this.shippingPostcode = Objects.requireNonNull(shippingPostcode, "shippingPostcode");
        this.shippingAddress1 = Objects.requireNonNull(shippingAddress1, "shippingAddress1");
        this.shippingAddress2 = shippingAddress2;
        this.memo = memo;
        this.items = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(items, "items")));
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(
            String orderNumber,
            Long userId,
            String currency,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            String receiverName,
            String receiverPhone,
            String shippingPostcode,
            String shippingAddress1,
            String shippingAddress2,
            String memo,
            List<OrderItem> items
    ) {
        Objects.requireNonNull(items, "items");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("주문 상품은 1개 이상이어야 합니다.");
        }

        BigDecimal subtotal = items.stream()
                .map(OrderItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal shipping = shippingFee == null ? BigDecimal.ZERO : shippingFee;
        BigDecimal total = subtotal.subtract(discount).add(shipping);

        return new Order(
                null,
                orderNumber,
                userId,
                OrderStatus.CREATED,
                currency,
                subtotal,
                discount,
                shipping,
                total,
                receiverName,
                receiverPhone,
                shippingPostcode,
                shippingAddress1,
                shippingAddress2,
                memo,
                items,
                null,
                null
        );
    }

    public static Order rehydrate(
            Long orderId,
            String orderNumber,
            Long userId,
            OrderStatus status,
            String currency,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String receiverName,
            String receiverPhone,
            String shippingPostcode,
            String shippingAddress1,
            String shippingAddress2,
            String memo,
            List<OrderItem> items,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Order(
                orderId,
                orderNumber,
                userId,
                status,
                currency,
                subtotalAmount,
                discountAmount,
                shippingFee,
                totalAmount,
                receiverName,
                receiverPhone,
                shippingPostcode,
                shippingAddress1,
                shippingAddress2,
                memo,
                items,
                createdAt,
                updatedAt
        );
    }

    public void cancel() {
        switch (this.status) {
            case CANCELLED -> {
                return;
            }
            case PAID -> throw new IllegalStateException("이미 결제가 완료된 주문입니다.");
            case PENDING_PAYMENT -> throw new IllegalStateException("결제가 진행 중인 주문은 취소할 수 없습니다.");
            case CREATED, PAYMENT_FAILED, PAYMENT_EXPIRED -> this.status = OrderStatus.CANCELLED;
        }
    }

    public void startPayment() {
        switch (this.status) {
            case PAID -> throw new IllegalStateException("이미 결제가 완료된 주문입니다.");
            case CANCELLED -> throw new IllegalStateException("이미 취소된 주문입니다.");
            case PENDING_PAYMENT -> {
                return;
            }
            case CREATED, PAYMENT_FAILED, PAYMENT_EXPIRED -> this.status = OrderStatus.PENDING_PAYMENT;
        }
    }

    public void markPaid() {
        switch (this.status) {
            case PAID -> {
                return;
            }
            case CANCELLED -> throw new IllegalStateException("이미 취소된 주문입니다.");
            case CREATED, PAYMENT_FAILED -> throw new IllegalStateException("결제가 시작되지 않은 주문입니다.");
            case PENDING_PAYMENT, PAYMENT_EXPIRED -> this.status = OrderStatus.PAID;
        }
    }

    public void markPaymentFailed() {
        switch (this.status) {
            case PAYMENT_FAILED -> {
                return;
            }
            case PAID -> throw new IllegalStateException("이미 결제가 완료된 주문입니다.");
            case CANCELLED -> throw new IllegalStateException("이미 취소된 주문입니다.");
            case CREATED -> throw new IllegalStateException("결제가 진행 중인 주문이 아닙니다.");
            case PENDING_PAYMENT, PAYMENT_EXPIRED -> this.status = OrderStatus.PAYMENT_FAILED;
        }
    }

    public void markPaymentExpired() {
        switch (this.status) {
            case PAYMENT_EXPIRED -> {
                return;
            }
            case PAID -> throw new IllegalStateException("이미 결제가 완료된 주문입니다.");
            case CANCELLED -> throw new IllegalStateException("이미 취소된 주문입니다.");
            case CREATED, PAYMENT_FAILED -> throw new IllegalStateException("결제가 진행 중인 주문이 아닙니다.");
            case PENDING_PAYMENT -> this.status = OrderStatus.PAYMENT_EXPIRED;
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getShippingPostcode() {
        return shippingPostcode;
    }

    public String getShippingAddress1() {
        return shippingAddress1;
    }

    public String getShippingAddress2() {
        return shippingAddress2;
    }

    public String getMemo() {
        return memo;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " 값은 0 이상이어야 합니다.");
        }
        return value;
    }
}
