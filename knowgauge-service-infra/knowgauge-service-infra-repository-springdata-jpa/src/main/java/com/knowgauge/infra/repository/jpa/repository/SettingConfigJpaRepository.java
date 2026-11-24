package com.knowgauge.infra.repository.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.SettingConfigEntity;

@Repository
public interface SettingConfigJpaRepository extends JpaRepository<SettingConfigEntity, String> {
}
