package com.msashop.user.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.user.application.port.out.DisableAuthUserPort;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateMeServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private DisableAuthUserPort disableAuthUserPort;

    private DeactivateMeService service;

    @BeforeEach
    void setUp() {
        service = new DeactivateMeService(loadUserPort, saveUserPort, disableAuthUserPort);
    }

    @Test
    void should_throw_not_found_when_user_is_missing() {
        // 비활성화 대상 사용자가 없으면 auth-service 호출 없이 COMMON_NOT_FOUND 로 종료한다.
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deactivateMe(20L));

        assertEquals(CommonErrorCode.COMMON_NOT_FOUND, ex.errorCode());
        verify(saveUserPort, never()).deactivate(org.mockito.ArgumentMatchers.any());
        verify(disableAuthUserPort, never()).disableAuthUser(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void should_deactivate_user_and_disable_auth_user_when_user_exists() {
        // 사용자 프로필 비활성화 후 auth-service 계정도 비활성화 요청을 보낸다.
        User user = user();
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.of(user));

        service.deactivateMe(20L);

        InOrder inOrder = inOrder(saveUserPort, disableAuthUserPort);
        inOrder.verify(saveUserPort).deactivate(user);
        inOrder.verify(disableAuthUserPort).disableAuthUser(20L);
    }

    /**
     * 현재 로그인 사용자에 대응하는 도메인 객체를 단순하게 구성한다.
     */
    private User user() {
        return new User(
                10L,
                20L,
                "홍길동",
                "EMP-001",
                "백엔드",
                "010-1234-5678",
                true,
                Instant.parse("2026-03-30T00:00:00Z"),
                1L,
                Instant.parse("2026-03-31T00:00:00Z"),
                1L
        );
    }
}
