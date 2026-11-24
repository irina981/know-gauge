package com.knowgauge.core.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	public List<TenantSummary> listTenants() {
		return tenantRepository.findTenantSummaries();
	}
}
