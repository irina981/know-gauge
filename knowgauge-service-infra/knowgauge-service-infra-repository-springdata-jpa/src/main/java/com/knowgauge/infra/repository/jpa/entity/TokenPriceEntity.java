package com.knowgauge.infra.repository.jpa.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "token_prices")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TokenPriceEntity extends AuditableEntity {

	@Column(nullable = false, length = 100, unique = true)
	private String model;

	@Column(name = "input_usd_per_million", nullable = false, precision = 12, scale = 6)
	private BigDecimal inputUsdPerMillion;

	@Column(name = "output_usd_per_million", nullable = false, precision = 12, scale = 6)
	private BigDecimal outputUsdPerMillion;
}
