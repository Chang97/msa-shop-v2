package com.msashop.user.query.application.port.out.model;

import java.time.Instant;

/**
 * DB에서 조인으로 뽑은 "Row"를 application 레이어에서 다루기 위한 타입.
 * - adapter(out) 구현체(JPA/native)가 무엇이든, application은 이 타입만 본다.
 */
public interface UserWithRoleRow {
    Long getUserId();
    String getEmail();
    String getLoginId();
    String getUserName();
    String getEmpNo();
    String getPstnName();
    String getTel();
    Boolean getUseYn();

    Instant getCreatedAt();
    String getCreatedBy();
    Instant getUpdatedAt();
    String getUpdatedBy();

    String getRoleName();
}