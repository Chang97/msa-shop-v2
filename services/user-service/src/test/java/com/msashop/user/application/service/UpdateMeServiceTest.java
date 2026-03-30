package com.msashop.user.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.user.application.port.in.model.UpdateMeCommand;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.port.out.SaveUserPort;
import com.msashop.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMeServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    private UpdateMeService service;

    @BeforeEach
    void setUp() {
        service = new UpdateMeService(loadUserPort, saveUserPort);
    }

    @Test
    void should_throw_not_found_when_user_is_missing() {
        // 수정 대상 사용자가 없으면 저장 없이 COMMON_NOT_FOUND 로 종료한다.
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateMe(20L, new UpdateMeCommand("홍길동", "EMP-001", "백엔드", "010-1111-2222"))
        );

        assertEquals(CommonErrorCode.COMMON_NOT_FOUND, ex.errorCode());
        verify(saveUserPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void should_throw_conflict_when_user_is_deactivated() {
        // 이미 비활성화된 사용자는 프로필 수정이 불가능하다.
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.of(user(false)));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateMe(20L, new UpdateMeCommand("홍길동", "EMP-001", "백엔드", "010-1111-2222"))
        );

        assertEquals(CommonErrorCode.COMMON_CONFLICT, ex.errorCode());
        verify(saveUserPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void should_update_profile_and_keep_existing_values_for_null_fields() {
        // 활성 사용자면 전달된 필드만 수정하고 null 필드는 기존 값을 유지한 채 저장한다.
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.of(user(true)));

        service.updateMe(20L, new UpdateMeCommand("김철수", null, "플랫폼", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("김철수", saved.getUserName());
        assertEquals("EMP-001", saved.getEmpNo());
        assertEquals("플랫폼", saved.getPstnName());
        assertEquals("010-1234-5678", saved.getTel());
    }

    /**
     * 수정/비활성화 유스케이스에서 재사용할 도메인 사용자 샘플이다.
     */
    private User user(boolean useYn) {
        return new User(
                10L,
                20L,
                "홍길동",
                "EMP-001",
                "백엔드",
                "010-1234-5678",
                useYn,
                Instant.parse("2026-03-30T00:00:00Z"),
                1L,
                Instant.parse("2026-03-31T00:00:00Z"),
                1L
        );
    }
}
