package com.msashop.user.command.adapter.out.persistence.repo;

import com.msashop.user.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCommandJpaRepository extends JpaRepository<UserJpaEntity, Long> { }