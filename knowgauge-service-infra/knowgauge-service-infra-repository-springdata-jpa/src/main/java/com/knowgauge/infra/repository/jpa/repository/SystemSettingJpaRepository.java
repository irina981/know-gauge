package com.knowgauge.infra.repository.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.SystemSettingEntity;

@Repository
public interface SystemSettingJpaRepository extends JpaRepository<SystemSettingEntity, Long> {

	Optional<SystemSettingEntity> findByKey(String key);
}
