package com.msashop.auth.application.service;

import com.msashop.auth.config.auth.LoginLockProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * loginId 기준 로그인 실패 횟수와 잠금 상태를 Redis에 관리하는 서비스다.
 *
 * 키 구성:
 * - {prefix}:{loginId}:fail : 현재 실패 횟수
 * - {prefix}:{loginId}:lock : 잠금 상태
 *
 * 동작 방식:
 * - 실패할 때마다 fail 키를 증가시킨다.
 * - 첫 실패 시 TTL을 설정해 실패 윈도우를 제한한다.
 * - 임계치에 도달하면 lock 키를 만들고 fail 키는 제거한다.
 * - 성공 로그인 시 fail 키를 삭제한다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptLockService {

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginLockProperties loginLockProperties;

    /**
     * 현재 loginId가 잠금 상태인지 확인한다.
     */
    public boolean isLocked(String loginId) {
        return stringRedisTemplate.hasKey(lockKey(loginId));
    }

    /**
     * 로그인 실패를 한 번 기록한다.
     *
     * @return 이번 실패로 잠금 임계치에 도달했으면 true
     */
    public boolean recordFailure(String loginId) {
        String failKey = failKey(loginId);
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        Long failures = valueOperations.increment(failKey);

        if (failures == null) {
            return false;
        }

        if (failures == 1L) {
            stringRedisTemplate.expire(failKey, lockDuration());
        }

        if (failures >= loginLockProperties.maxFailures()) {
            valueOperations.set(lockKey(loginId), "1", lockDuration());
            stringRedisTemplate.delete(failKey);
            return true;
        }

        return false;
    }

    /**
     * 성공 로그인 후 누적된 실패 횟수를 초기화한다.
     */
    public void clearFailures(String loginId) {
        stringRedisTemplate.delete(failKey(loginId));
    }

    private Duration lockDuration() {
        return Duration.ofSeconds(loginLockProperties.lockSeconds());
    }

    private String failKey(String loginId) {
        return loginLockProperties.keyPrefix() + ":" + loginId + ":fail";
    }

    private String lockKey(String loginId) {
        return loginLockProperties.keyPrefix() + ":" + loginId + ":lock";
    }
}
