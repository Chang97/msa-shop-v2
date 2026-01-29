package com.msashop.user.adapter.out.persistence.mapper;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.domain.model.User;

/**
 * JPA Entity <-> Domain 매핑.
 *
 * - 조회(load) 시 entity -> domain
 * - 저장(save) 시 domain -> entity(기존 entity에 반영)
 */
public final class UserEntityMapper {

    private UserEntityMapper() {}

    /**
     * Entity -> Domain (rehydrate)
     * - 생성자 계약에 맞춰 "최소 상태"로 복원.
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
                e.getUpdatedAt(),
                e.getUpdatedBy()
        );
    }

    /**
     * Domain 상태를 기존 엔티티에 반영한다.
     */
    public static void applyTo(User domain, UserJpaEntity entity) {
        entity.changeUseYn(domain.isUseYn());
        entity.changeUserName(domain.getUserName());
        entity.changeEmpNo(domain.getEmpNo());
        entity.changePstnName(domain.getPstnName());
        entity.changeTel(domain.getTel());
    }
}

