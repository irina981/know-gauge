package com.knowgauge.core.service.admin;

import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.port.repository.TenantRepository;

@Service
public class TenantAdminServiceImpl implements TenantAdminService {

	private final TenantRepository tenantRepository;

	public TenantAdminServiceImpl(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
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
		return tenantRepository.findTenantSummaries(search, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public TenantSummary getTenant(Long tenantId) {
		return tenantRepository.findTenantSummary(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant " + tenantId + " not found."));
	}
}
