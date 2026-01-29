package com.msashop.user.application.port.in.model;

/**
 * Application 레이어의 command 모델.
 * - web dto를 application이 직접 참조하지 않게 하기 위함.
 */
public record UpdateMeCommand(
        String userName,
        String empNo,
        String psntName,
        String tel
) {
}
