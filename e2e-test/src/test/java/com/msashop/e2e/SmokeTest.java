package com.msashop.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 실행 환경이 기본적으로 동작하는지 확인하는 최소 스모크 테스트다.
class SmokeTest {

    // 테스트 러너가 정상 동작하는지 검증한다.
    @Test
    void smoke() {
        assertTrue(true);
    }
}
