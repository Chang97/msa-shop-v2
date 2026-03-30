package com.msashop.user.adapter.out.persistence.mapper;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.domain.model.User;

/**
 * Maps between JPA entities and the user domain aggregate.
 */
public final class UserEntityMapper {

    private UserEntityMapper() {}

    /**
     * Rehydrates the domain aggregate from the persistence entity.
     */
    public static User toDomain(UserJpaEntity e) {
        return new User(
                e.getUserId(),
                e.getAuthUserId(),
                e.getUserName(),
                e.getEmpNo(),
                e.getPstnName(),
                e.getTel(),
                Boolean.TRUE.equals(e.getUseYn()),
                e.getCreatedAt(),
                e.getCreatedBy(),
                e.getUpdatedAt(),
                e.getUpdatedBy()
        );
    }

    /**
     * Applies mutable domain state back to the existing entity.
     */
    public static void applyTo(User domain, UserJpaEntity entity) {
        entity.changeUseYn(domain.isUseYn());
        entity.changeUserName(domain.getUserName());
        entity.changeEmpNo(domain.getEmpNo());
        entity.changePstnName(domain.getPstnName());
        entity.changeTel(domain.getTel());
    }
}
