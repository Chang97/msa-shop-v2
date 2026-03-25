package com.msashop.common.event;

import java.time.Instant;

/**
 * saga consumer에서 반복되는 processed_event claim 흐름을 감싼다.
 */
public final class SagaClaimExecutor {

    private SagaClaimExecutor() {
    }

    public static boolean execute(
            long claimTimeoutSeconds,
            ClaimAction claimAction,
            ThrowingWork work,
            ReleaseAction releaseAction
    ) throws Exception {
        Instant now = Instant.now();

        boolean claimed = claimAction.claim(now, now.minusSeconds(claimTimeoutSeconds));
        if (!claimed) {
            // 이미 다른 worker가 같은 이벤트를 선점했거나 처리 완료한 상태다.
            // 현재 consumer는 Kafka 레코드를 안전하게 ack 해도 된다.
            return true;
        }

        try {
            return work.run();
        } catch (InvalidSagaMessageException e) {
            // poison message는 claim을 다시 풀지 않고 DLQ로 보내야 한다.
            throw e;
        } catch (Exception e) {
            // 인프라 예외는 claim을 풀어야 다음 재시도가 다시 집을 수 있다.
            releaseAction.release(e.getMessage());
            throw e;
        }
    }

    @FunctionalInterface
    public interface ClaimAction {
        boolean claim(Instant now, Instant staleThreshold);
    }

    @FunctionalInterface
    public interface ReleaseAction {
        void release(String lastError);
    }

    @FunctionalInterface
    public interface ThrowingWork {
        boolean run() throws Exception;
    }
}
