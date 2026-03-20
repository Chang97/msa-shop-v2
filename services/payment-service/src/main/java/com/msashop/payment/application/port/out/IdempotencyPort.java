package com.msashop.payment.application.port.out;

import java.time.Duration;

public interface IdempotencyPort {
    boolean tryAcquire(String key, Duration ttl);

    void release(String key);

    boolean exists(String key);
}
