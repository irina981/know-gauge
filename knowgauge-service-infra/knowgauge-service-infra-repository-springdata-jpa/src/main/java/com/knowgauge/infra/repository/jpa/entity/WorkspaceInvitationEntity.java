package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;

import com.knowgauge.core.model.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "workspace_invitations")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
public class WorkspaceInvitationEntity extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private Long tenantId;

	@Column(nullable = false, length = 320, updatable = false)
	private String email;

	@Column(name = "role_code", nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(name = "public_token", nullable = false, unique = true, length = 64, updatable = false)
	private String publicToken;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "send_count", nullable = false)
	@Builder.Default
	private Integer sendCount = 0;

	@Column(name = "send_error", columnDefinition = "text")
	private String sendError;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "accepted_user_id")
	private Long acceptedUserId;
}
