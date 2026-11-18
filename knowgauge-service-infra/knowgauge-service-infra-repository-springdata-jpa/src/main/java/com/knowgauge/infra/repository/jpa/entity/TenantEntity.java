package com.knowgauge.infra.repository.jpa.entity;

import com.knowgauge.core.model.enums.TenantType;

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
@Table(name = "tenants")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TenantEntity extends AuditableEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TenantType type;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "owner_user_id")
	private Long ownerUserId;
}
