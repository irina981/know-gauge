package com.knowgauge.infra.repository.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TenantEntity;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, Long> {
}
