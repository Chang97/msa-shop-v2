package com.msashop.payment.adapter.out.redis;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.payment.application.port.out.IdempotencyPort;
import com.msashop.payment.config.redis.IdempotencyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyAdapter implements IdempotencyPort {
    private final StringRedisTemplate redis;
    private final IdempotencyProperties properties;

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        String namespaced = properties.keyPrefix() + key;
        Boolean ok = redis.opsForValue().setIfAbsent(namespaced, "1", ttl);
        if (ok == null) throw new ConflictException(CommonErrorCode.COMMON_CONFLICT);
        return ok;
    }

    @Override
    public void release(String key) {
        redis.delete(properties.keyPrefix() + key);
    }

    @Override
    public boolean exists(String key) {
        return false;
    }
}
