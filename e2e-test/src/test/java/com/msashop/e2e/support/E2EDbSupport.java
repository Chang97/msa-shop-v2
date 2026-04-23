package com.msashop.e2e.support;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * E2E 테스트에서 API만으로 만들기 어렵거나 확인하기 어려운 내부 상태를 보조하는 DB 헬퍼다.
 * 시간 기반 스케줄러 시나리오를 빠르게 만들거나, 외부 API에 노출되지 않는 결제 내부 상태를 검증할 때만 사용한다.
 */
public class E2EDbSupport {

    private static final String ORDER_DB_URL = System.getenv().getOrDefault(
            "ORDER_DB_URL",
            "jdbc:postgresql://localhost:15432/order_db"
    );
    private static final String ORDER_DB_USERNAME = System.getenv().getOrDefault("ORDER_DB_USERNAME", "postgres");
    private static final String ORDER_DB_PASSWORD = System.getenv().getOrDefault("ORDER_DB_PASSWORD", "postgres");

    private static final String PAYMENT_DB_URL = System.getenv().getOrDefault(
            "PAYMENT_DB_URL",
            "jdbc:postgresql://localhost:15432/payment_db"
    );
    private static final String PAYMENT_DB_USERNAME = System.getenv().getOrDefault("PAYMENT_DB_USERNAME", "postgres");
    private static final String PAYMENT_DB_PASSWORD = System.getenv().getOrDefault("PAYMENT_DB_PASSWORD", "postgres");

    /**
     * 주문 만료/재결제처럼 특정 상태와 과거 updated_at 이 필요한 시나리오를 만들기 위해 주문 상태를 직접 조정한다.
     */
    public void updateOrderStatusAndUpdatedAt(Long orderId, String status, Instant updatedAt) {
        String sql = """
                update orders
                   set status = ?, updated_at = ?
                 where id = ?
                """;

        try (var connection = DriverManager.getConnection(ORDER_DB_URL, ORDER_DB_USERNAME, ORDER_DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setTimestamp(2, Timestamp.from(updatedAt));
            statement.setLong(3, orderId);

            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("주문 상태 업데이트 대상이 1건이 아닙니다. orderId=" + orderId);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("order_db 상태 조작에 실패했습니다. orderId=" + orderId, e);
        }
    }

    /**
     * reconciliation 테스트에서 APPROVAL_UNKNOWN 이 최종 APPROVED/FAILED 로 수렴하는지 payment_db 내부 상태를 조회한다.
     */
    public String findPaymentStatusByIdempotencyKey(String idempotencyKey) {
        String sql = """
                select status
                  from payment_transaction
                 where idempotency_key = ?
                """;

        try (var connection = DriverManager.getConnection(PAYMENT_DB_URL, PAYMENT_DB_USERNAME, PAYMENT_DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);

            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getString("status");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("payment_db 상태 조회에 실패했습니다. idempotencyKey=" + idempotencyKey, e);
        }
    }
}
