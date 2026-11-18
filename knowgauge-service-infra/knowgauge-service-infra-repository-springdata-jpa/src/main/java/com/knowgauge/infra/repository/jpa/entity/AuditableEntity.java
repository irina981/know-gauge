package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Audit columns, filled by Spring Data auditing ({@link AuditingEntityListener}): the dates, and the user from
 * the execution context (the signed-in user, the system user for jobs, or {@code null} when nobody is signed in).
 */
@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends PersistedEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	protected Instant createdAt;

	@CreatedBy
	@Column(name = "created_by", updatable = false)
	protected Long createdBy;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	protected Instant updatedAt;

	@LastModifiedBy
	@Column(name = "updated_by")
	protected Long updatedBy;
}
