package com.msashop.user.unit.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.UserErrorCode;
import com.msashop.user.application.port.in.model.UserResult;
import com.msashop.user.application.port.out.LoadUserPort;
import com.msashop.user.application.service.GetMeService;
import com.msashop.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMeServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    private GetMeService service;

    @BeforeEach
    void setUp() {
        service = new GetMeService(loadUserPort);
    }

    @Test
    void should_return_user_result_when_profile_exists() {
        // 현재 사용자 프로필이 존재하면 도메인 객체를 응답 모델로 변환해 반환한다.
        Instant createdAt = Instant.parse("2026-03-31T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-03-31T01:00:00Z");
        User user = user(
                10L,
                20L,
                "홍길동",
                "EMP-001",
                "백엔드",
                "010-1234-5678",
                true,
                createdAt,
                1L,
                updatedAt,
                2L
        );
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.of(user));

        UserResult result = service.getMe(20L);

        assertEquals(10L, result.userId());
        assertEquals(20L, result.authUserId());
        assertEquals("홍길동", result.userName());
        assertEquals("EMP-001", result.empNo());
        assertEquals("백엔드", result.pstnName());
        assertEquals("010-1234-5678", result.tel());
        assertEquals(createdAt, result.createdAt());
        assertEquals(updatedAt, result.updatedAt());
    }

    @Test
    void should_throw_user_not_found_when_profile_is_missing() {
        // 현재 사용자에 대응하는 프로필이 없으면 USER_NOT_FOUND 를 반환한다.
        when(loadUserPort.findByAuthUserId(20L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getMe(20L));

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode());
    }

    /**
     * 조회 테스트에서 재사용할 도메인 사용자 샘플이다.
     */
    private User user(
            Long userId,
            Long authUserId,
            String userName,
            String empNo,
            String pstnName,
            String tel,
            boolean useYn,
            Instant createdAt,
            Long createdBy,
            Instant updatedAt,
            Long updatedBy
    ) {
        return new User(
                userId,
                authUserId,
                userName,
                empNo,
                pstnName,
                tel,
                useYn,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy
        );
    }
}
