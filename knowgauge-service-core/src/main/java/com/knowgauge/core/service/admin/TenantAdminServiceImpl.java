package com.knowgauge.core.service.admin;

import java.time.Clock;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.service.settings.SystemSettingsService;
import com.knowgauge.core.service.usage.UsagePeriod;

@Service
public class TenantAdminServiceImpl implements TenantAdminService {

	private final TenantRepository tenantRepository;
	private final SystemSettingsService systemSettings;
	private final Clock clock;

	@Autowired
	public TenantAdminServiceImpl(TenantRepository tenantRepository, SystemSettingsService systemSettings) {
		this(tenantRepository, systemSettings, Clock.systemUTC());
	}

	TenantAdminServiceImpl(TenantRepository tenantRepository, SystemSettingsService systemSettings, Clock clock) {
		this.tenantRepository = tenantRepository;
		this.systemSettings = systemSettings;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TenantSummary> listTenants(TenantSearch search, Pageable pageable) {
		// Checked here: thrown from the repository it would be wrapped in a persistence exception (500, not 400).
		for (Sort.Order order : pageable.getSort()) {
			if (!TenantSearch.SORTABLE.contains(order.getProperty())) {
				throw new IllegalArgumentException("Cannot sort tenants by '" + order.getProperty() + "'. Sortable: "
						+ String.join(", ", new TreeSet<>(TenantSearch.SORTABLE)));
			}
		}
		// Tokens this month (UTC), like the usage page and the limit check, with the limits every tenant shares.
		UsagePeriod month = UsagePeriod.currentMonth(clock);
		Long storageLimit = systemSettings.storageLimitBytes().orElse(null);
		Long tokenLimit = systemSettings.monthlyTokenLimit().orElse(null);
		return tenantRepository.findTenantSummaries(search, pageable, month.from(), month.to())
				.map(summary -> summary.withLimits(storageLimit, tokenLimit));
	}

	@Override
	@Transactional(readOnly = true)
	public TenantSummary getTenant(Long tenantId) {
		UsagePeriod month = UsagePeriod.currentMonth(clock);
		return tenantRepository.findTenantSummary(tenantId, month.from(), month.to())
				.map(summary -> summary.withLimits(systemSettings.storageLimitBytes().orElse(null),
						systemSettings.monthlyTokenLimit().orElse(null)))
				.orElseThrow(() -> new ResourceNotFoundException("Tenant " + tenantId + " not found."));
	}
}
