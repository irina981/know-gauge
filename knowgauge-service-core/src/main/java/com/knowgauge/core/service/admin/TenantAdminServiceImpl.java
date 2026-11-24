package com.knowgauge.core.service.admin;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.core.model.WorkspaceLimits;
import com.knowgauge.core.service.usage.UsagePeriod;

@Service
public class TenantAdminServiceImpl implements TenantAdminService {

	private final TenantRepository tenantRepository;
	private final TenantSettingsService tenantSettings;
	private final Clock clock;

	@Autowired
	public TenantAdminServiceImpl(TenantRepository tenantRepository, TenantSettingsService tenantSettings) {
		this(tenantRepository, tenantSettings, Clock.systemUTC());
	}

	TenantAdminServiceImpl(TenantRepository tenantRepository, TenantSettingsService tenantSettings, Clock clock) {
		this.tenantRepository = tenantRepository;
		this.tenantSettings = tenantSettings;
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
		// Tokens this month (UTC), like the usage page and the limit check, with each workspace's limits in effect.
		UsagePeriod month = UsagePeriod.currentMonth(clock);
		Page<TenantSummary> page = tenantRepository.findTenantSummaries(search, pageable, month.from(), month.to());
		List<Long> ids = page.getContent().stream().map(TenantSummary::tenantId).toList();
		Map<Long, WorkspaceLimits> limits = tenantSettings.limits(ids);
		Set<Long> ownLimits = tenantSettings.withOwnValues(ids);
		return page.map(summary -> summary.withLimits(limits.get(summary.tenantId()),
				ownLimits.contains(summary.tenantId())));
	}

	@Override
	@Transactional(readOnly = true)
	public TenantSummary getTenant(Long tenantId) {
		UsagePeriod month = UsagePeriod.currentMonth(clock);
		return tenantRepository.findTenantSummary(tenantId, month.from(), month.to())
				.map(summary -> summary.withLimits(tenantSettings.limits(tenantId),
						!tenantSettings.withOwnValues(List.of(tenantId)).isEmpty()))
				.orElseThrow(() -> new ResourceNotFoundException("Tenant " + tenantId + " not found."));
	}
}
