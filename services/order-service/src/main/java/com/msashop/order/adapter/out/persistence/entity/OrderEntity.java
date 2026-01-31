package com.msashop.order.adapter.out.persistence.entity;

import com.msashop.order.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 32, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "receiver_name", nullable = false, length = 80)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 32)
    private String receiverPhone;

    @Column(name = "shipping_postcode", nullable = false, length = 10)
    private String shippingPostcode;

    @Column(name = "shipping_address1", nullable = false, length = 255)
    private String shippingAddress1;

    @Column(name = "shipping_address2", length = 255)
    private String shippingAddress2;

    @Column(name = "memo", length = 255)
    private String memo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    public void setItems(List<OrderItemEntity> items) {
        this.items.clear();
        if (items != null) {
            items.forEach(i -> i.setOrder(this));
            this.items.addAll(items);
        }
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }
}

