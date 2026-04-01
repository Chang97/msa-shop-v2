package com.msashop.auth.unit.application.service;

import com.msashop.auth.application.service.LoginAttemptLockService;
import com.msashop.auth.config.auth.LoginLockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptLockServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptLockService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptLockService(
                stringRedisTemplate,
                new LoginLockProperties("auth:login-lock", 5L, 600L)
        );
    }

    @Test
    @DisplayName("lock 키가 있으면 해당 loginId는 잠금 상태로 본다")
    void should_return_true_when_login_id_is_locked() {
        when(stringRedisTemplate.hasKey("auth:login-lock:tester:lock")).thenReturn(true);

        assertTrue(service.isLocked("tester"));
    }

    @Test
    @DisplayName("첫 실패 시 fail 키를 만들고 실패 윈도우 TTL을 설정한다")
    void should_set_counter_ttl_on_first_failure() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login-lock:tester:fail")).thenReturn(1L);

        boolean locked = service.recordFailure("tester");

        assertFalse(locked);
        verify(stringRedisTemplate).expire("auth:login-lock:tester:fail", Duration.ofSeconds(600));
    }

    @Test
    @DisplayName("임계치에 도달하면 lock 키를 만들고 fail 키는 제거한다")
    void should_create_lock_key_and_clear_fail_counter_when_threshold_is_reached() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login-lock:tester:fail")).thenReturn(5L);

        boolean locked = service.recordFailure("tester");

        assertTrue(locked);
        verify(valueOperations).set("auth:login-lock:tester:lock", "1", Duration.ofSeconds(600));
        verify(stringRedisTemplate).delete("auth:login-lock:tester:fail");
    }

    @Test
    @DisplayName("성공 로그인 후에는 fail 키를 삭제한다")
    void should_clear_failure_counter_after_successful_login() {
        service.clearFailures("tester");

        verify(stringRedisTemplate).delete("auth:login-lock:tester:fail");
    }
}
