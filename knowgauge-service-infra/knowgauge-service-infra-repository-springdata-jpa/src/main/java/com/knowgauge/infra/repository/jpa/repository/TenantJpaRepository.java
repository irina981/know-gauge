package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.infra.repository.jpa.entity.TenantEntity;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, Long> {

	@Query("select t.id from TenantEntity t where t.type = :type order by t.id")
	List<Long> findIdsByType(@Param("type") TenantType type);
}
