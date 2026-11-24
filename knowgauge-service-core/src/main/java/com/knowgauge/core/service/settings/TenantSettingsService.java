package com.knowgauge.core.service.settings;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.knowgauge.core.model.TenantLimits;

/**
 * Settings of one tenant, ADR014: for every {@link SettingDefinition#tenantLevel() tenant-level}
 * setting the tenant may have a value of its own, which overrides the system value. Kept by an {@code ADMIN} on
 * the Workspaces page.
 */
public interface TenantSettingsService {

	/**
	 * A tenant-level setting of one tenant.
	 *
	 * @param systemValue the system value in effect ({@code null} when empty)
	 * @param ownValue    the tenant's own value; {@code null} when it uses the system value
	 * @param value       the value in effect for the tenant
	 */
	record TenantSettingValue(SettingDefinition definition, String systemValue, String ownValue, String value) {

		public boolean overridden() {
			return ownValue != null;
		}
	}

	/**
	 * @return the tenant-level settings of the tenant, in page order
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException when there is no such tenant
	 */
	List<TenantSettingValue> list(Long tenantId);

	/**
	 * Saves the tenant's own values. All values are checked first; if one does not fit, nothing is saved.
	 *
	 * @param values new values by key; an empty value removes the tenant's own value, so the system value applies
	 * @return the tenant's settings afterwards
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException for an unknown tenant or key
	 * @throws IllegalArgumentException for a system-only key, or naming every value that does not fit
	 */
	List<TenantSettingValue> setAll(Long tenantId, Map<String, String> values);

	/**
	 * Removes every value of the tenant's own, so the system values apply to it again.
	 *
	 * @return the tenant's settings afterwards
	 */
	List<TenantSettingValue> resetAll(Long tenantId);

	/** Gives the tenant no limits (0 for every limit), e.g. an application admin's own tenant. */
	void setUnlimited(Long tenantId);

	/** @return the limits in effect for the tenant */
	TenantLimits limits(Long tenantId);

	/** @return the limits in effect for each of the tenants */
	Map<Long, TenantLimits> limits(Collection<Long> tenantIds);

	/** @return the tenants among the given ones that have a value of their own for any setting */
	Set<Long> withOwnValues(Collection<Long> tenantIds);
}
