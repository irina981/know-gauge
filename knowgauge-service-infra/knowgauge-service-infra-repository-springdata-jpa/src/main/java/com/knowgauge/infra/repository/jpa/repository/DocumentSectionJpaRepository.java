package com.knowgauge.infra.repository.jpa.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.DocumentSectionEntity;

/** Sections have no tenant column: they are scoped through their document. */
@Repository
public interface DocumentSectionJpaRepository extends JpaRepository<DocumentSectionEntity, Long> {

	@Query("""
			select s from DocumentSectionEntity s, DocumentEntity d
			 where d.id = s.documentId and d.tenantId = :tenantId and s.id = :id
			""")
	Optional<DocumentSectionEntity> findByTenantIdAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

	@Query(value = """
			select s from DocumentSectionEntity s, DocumentEntity d
			 where d.id = s.documentId and d.tenantId = :tenantId and s.documentId = :documentId
			""", countQuery = """
			select count(s) from DocumentSectionEntity s, DocumentEntity d
			 where d.id = s.documentId and d.tenantId = :tenantId and s.documentId = :documentId
			""")
	Page<DocumentSectionEntity> findByTenantIdAndDocumentId(@Param("tenantId") Long tenantId,
			@Param("documentId") Long documentId, Pageable pageable);
}
