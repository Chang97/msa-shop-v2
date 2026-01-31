package com.msashop.order.adapter.out.persistence.entity;

import com.msashop.order.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "changed_by")
    private Long changedBy;
}

