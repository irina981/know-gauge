package com.knowgauge.infra.vectorstore.pgvector.jpa.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
	@Column(name = "created_by", updatable = false, nullable = false)
	protected Long createdBy;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	protected Instant updatedAt;

	@LastModifiedBy
	@Column(name = "updated_by", nullable = false)
	protected Long updatedBy;

	// TODO: Replace with SpringBoot audit
	@PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        createdBy = 1L;
        updatedBy = 1L;
    }

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
		updatedBy = 1L;
	}
	// END TODO: Replace with SpringBoot audit
}
