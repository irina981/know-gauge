package com.knowgauge.core.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * What one LLM model costs, a system-wide setting kept by an {@code ADMIN}. {@code model} matches the model name of a
 * recorded LLM call exactly or as a prefix (the provider often returns a dated name such as {@code gpt-4.1-2025-04-14});
 * the longest matching price wins.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TokenPrice extends AuditableObject {
	private String model;
	/** US dollars per million input (prompt) tokens. */
	private BigDecimal inputUsdPerMillion;
	/** US dollars per million output (completion) tokens. */
	private BigDecimal outputUsdPerMillion;
}
