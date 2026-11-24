package com.knowgauge.infra.repository.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.infra.repository.jpa.entity.SettingEntity;

@Repository
public interface SettingJpaRepository extends JpaRepository<SettingEntity, Long> {

	List<SettingEntity> findByType(SettingType type);

	Optional<SettingEntity> findByTypeAndKey(SettingType type, String key);

	List<SettingEntity> findByTypeAndTenantId(SettingType type, Long tenantId);

	List<SettingEntity> findByTypeAndTenantIdIn(SettingType type, Collection<Long> tenantIds);

	@Modifying
	@Query("delete from SettingEntity s where s.type = :type")
	int deleteByType(@Param("type") SettingType type);

	@Modifying
	@Query("delete from SettingEntity s where s.type = com.knowgauge.core.model.enums.SettingType.TENANT"
			+ " and s.tenantId = :tenantId")
	int deleteTenantRows(@Param("tenantId") Long tenantId);
}
