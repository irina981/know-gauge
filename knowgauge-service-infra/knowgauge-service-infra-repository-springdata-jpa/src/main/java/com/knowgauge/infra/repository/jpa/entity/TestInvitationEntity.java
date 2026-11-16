package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "test_invitations")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
public class TestInvitationEntity extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "test_id", nullable = false)
	private Long testId;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "public_token", nullable = false, unique = true, length = 64, updatable = false)
	private String publicToken;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "send_count", nullable = false)
	@Builder.Default
	private Integer sendCount = 0;

	@Column(name = "send_error", columnDefinition = "text")
	private String sendError;
}
