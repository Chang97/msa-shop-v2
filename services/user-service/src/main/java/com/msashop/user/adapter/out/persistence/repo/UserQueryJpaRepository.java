package com.msashop.user.adapter.out.persistence.repo;

import com.msashop.user.adapter.out.persistence.entity.UserJpaEntity;
import com.msashop.user.application.port.out.model.UserRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Query 전용 Repository.
 */
public interface UserQueryJpaRepository extends JpaRepository<UserJpaEntity, Long> {

        Optional<UserRow> findByAuthUserId(@Param("userId") Long userId);
}

