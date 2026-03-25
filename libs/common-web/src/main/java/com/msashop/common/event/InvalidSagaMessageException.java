package com.msashop.common.event;

/**
 * 메시지 스키마가 잘못되었거나 payload를 역직렬화할 수 없어
 * 재시도해도 성공 가능성이 낮은 poison message 상황을 표현한다.
 *
 * listener는 이 예외를 잡아 DLQ로 보내고 ack 한다.
 */
public class InvalidSagaMessageException extends RuntimeException {

    private final String reasonCode;

    public InvalidSagaMessageException(String reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
