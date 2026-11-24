package com.knowgauge.infra.repository.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.infra.repository.jpa.entity.SettingEntity;

@Repository
public interface SettingJpaRepository extends JpaRepository<SettingEntity, Long> {

	List<SettingEntity> findByOwner(SettingOwner owner);

	Optional<SettingEntity> findByOwnerAndKey(SettingOwner owner, String key);

	List<SettingEntity> findByOwnerAndOwnerId(SettingOwner owner, Long ownerId);

	List<SettingEntity> findByOwnerAndOwnerIdIn(SettingOwner owner, Collection<Long> ownerIds);

	@Modifying
	@Query("delete from SettingEntity s where s.owner = :owner")
	int deleteByOwner(@Param("owner") SettingOwner owner);

	@Modifying
	@Query("delete from SettingEntity s where s.owner = :owner and s.ownerId = :ownerId")
	int deleteOwnerRows(@Param("owner") SettingOwner owner, @Param("ownerId") Long ownerId);
}
