package com.msashop.common.event.payload;

public record UserProfileCreationFailedPayload(
        Long authUserId,
        String reasonCode,
        String reasonMessage
) {
}
