package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Type;

import com.knowgauge.core.model.UserPreferences;
import com.knowgauge.core.model.enums.Role;

import io.hypersistence.utils.hibernate.type.json.JsonType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

	/** {@code null} only for the system user (id 0). Changes when the user accepts a workspace invitation (ADR015). */
	@Column(name = "tenant_id")
	private Long tenantId;

	/** Rows of {@code user_roles} (ADR012); empty for the system user (id 0). Loaded with the user. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "role_code", length = 30, nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private Set<Role> roles = new HashSet<>();

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
