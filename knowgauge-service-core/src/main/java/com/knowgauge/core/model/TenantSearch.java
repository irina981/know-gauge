package com.knowgauge.core.model;

import java.util.Set;

import com.knowgauge.core.model.enums.TenantType;

/**
 * Filters of the admin tenant list (ADR013). Both optional: {@code text} matches the tenant name and the owner's
 * name or email, ignoring case; {@code type} keeps one tenant type.
 */
public record TenantSearch(String text, TenantType type) {

	/** Properties the list can be sorted by. */
	public static final Set<String> SORTABLE = Set.of("name", "type", "owner", "topics", "documents", "createdAt");
}
