package com.msashop.user.adapter.out.persistence.mapper;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.domain.model.User;

/**
 * JPA Entity <-> Domain 매핑.
 *
 * - 조회(load) 시: entity -> domain
 * - 저장(save) 시: domain -> entity(기존 entity에 반영)
 */
public final class UserEntityMapper {

    private UserEntityMapper() {}

    /**
     * Entity -> Domain (rehydrate)
     * - 영속 엔티티를 도메인 애그리거트로 복원한다.
     */
    public static User toDomain(UserJpaEntity e) {
        // 도메인 생성자 계약에 맞춰 "최소 상태"만 복원
        return new User(
                e.getUserId(),
                Boolean.TRUE.equals(e.getUseYn()),
                e.getUpdatedAt(),
                e.getUpdatedBy()
        );
    }

    /**
     * Domain 상태를 "기존 엔티티"에 반영한다.
     */
    public static void applyTo(User domain, UserJpaEntity entity) {
        entity.changeUseYn(domain.isUseYn());
        entity.changeUserName(domain.getUserName());
        entity.changeEmpNo(domain.getEmpNo());
        entity.changePstnName(domain.getPstnName());
        entity.changeTel(domain.getTel());
    }
}
