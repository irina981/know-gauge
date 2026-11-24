package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.infra.repository.jpa.entity.TenantEntity;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, Long> {

	@Query("select t.id from TenantEntity t where t.type = :type order by t.id")
	List<Long> findIdsByType(@Param("type") TenantType type);

	/** Every tenant with its owner and content counts (ADR013), straight into the domain record. */
	@Query("""
			select new com.knowgauge.core.model.TenantSummary(t.id, t.name, t.type, t.ownerUserId, u.displayName,
				u.email, t.createdAt,
				(select count(tp) from TopicEntity tp where tp.tenantId = t.id),
				(select count(d) from DocumentEntity d where d.tenantId = t.id))
			from TenantEntity t left join UserEntity u on u.id = t.ownerUserId
			order by lower(t.name), t.id
			""")
	List<TenantSummary> findTenantSummaries();
}
