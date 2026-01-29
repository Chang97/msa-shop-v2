package com.msashop.user.application.port.out.model;

import java.time.Instant;

/**
 * DB projection for user profile.
 */
public interface UserRow {
    Long getUserId();
    Long getAuthUserId();
    String getUserName();
    String getEmpNo();
    String getPstnName();
    String getTel();
    Boolean getUseYn();

    Instant getCreatedAt();
    Long getCreatedBy();
    Instant getUpdatedAt();
    Long getUpdatedBy();
}

