package com.msashop.e2e.support;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class PollingSupport {
    private PollingSupport() {}

    public static <T> T pollUntil(Duration timeout, Duration interval, Supplier<T> supplier, Predicate<T> done) {
        Instant deadline = Instant.now().plus(timeout);
        T last = null;

        while (Instant.now().isBefore(deadline)) {
            last = supplier.get();
            if (done.test(last)) {
                return last;
            }

            try {
                Thread.sleep(interval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalArgumentException("polling 대기 중 인터럽트 발생" , e);
            }
        }

        throw new AssertionError("제한 시간 안에 기대 조건에 도달하지 못했습니다. last=" + last);
    }
}
