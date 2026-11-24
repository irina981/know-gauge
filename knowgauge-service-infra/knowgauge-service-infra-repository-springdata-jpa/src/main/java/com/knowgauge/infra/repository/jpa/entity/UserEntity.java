package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;

import org.hibernate.annotations.Type;

import com.knowgauge.core.model.UserPreferences;
import com.knowgauge.core.model.enums.Role;

import io.hypersistence.utils.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserEntity extends AuditableEntity {

	@Column(name = "external_id", nullable = false, unique = true, updatable = false)
	private String externalId;

	/** {@code null} only for the system user (id 0). */
	@Column(name = "tenant_id", updatable = false)
	private Long tenantId;

	/** {@code null} only for the system user (id 0). */
	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private Role role;

	@Column(length = 320)
	private String email;

	@Column(name = "display_name", length = 200)
	private String displayName;

	@Column(name = "last_seen_at")
	private Instant lastSeenAt;

	/** {@code null} until the user changes a setting. */
	@Column(name = "preferences_json", columnDefinition = "jsonb")
	@Type(JsonType.class)
	private UserPreferences preferences;
}
