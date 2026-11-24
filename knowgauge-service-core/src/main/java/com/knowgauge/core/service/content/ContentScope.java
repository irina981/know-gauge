package com.knowgauge.core.service.content;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.ApplicationAdminProvider;
import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.context.TenantViewProvider;
import com.knowgauge.core.exception.ReadOnlyContentException;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;

/**
 * Which tenants' content (topics, documents, chunks, embeddings) the current work may read and change (ADR010).
 * <ul>
 * <li>Read: the own tenant and every {@link TenantType#LIBRARY} tenant.</li>
 * <li>Change: the own tenant; library content only for an {@code ADMIN}. Jobs and startup seeding
 * run in the library tenant itself, so it is their own tenant.</li>
 * <li>An {@code ADMIN} viewing another user's library (tenant) reads that tenant (it is the current tenant then) and
 * changes nothing (ADR013).</li>
 * </ul>
 * Library tenants are created by migrations, so their ids are read once and kept for the life of the application.
 */
@Component
public class ContentScope {

	private final TenantRepository tenantRepository;
	private final ExecutionContext executionContext;
	private final ApplicationAdminProvider adminProvider;
	private final TenantViewProvider tenantView;
	private volatile List<Long> libraryTenantIds;

	public ContentScope(TenantRepository tenantRepository, ExecutionContext executionContext,
			ApplicationAdminProvider adminProvider, TenantViewProvider tenantView) {
		this.tenantRepository = tenantRepository;
		this.executionContext = executionContext;
		this.adminProvider = adminProvider;
		this.tenantView = tenantView;
	}

	/** The current tenant first, then the library tenants. */
	public List<Long> readableTenantIds() {
		return readableTenantIds(executionContext.tenantId());
	}

	/** {@code tenantId} first, then the library tenants (without duplicates). */
	public List<Long> readableTenantIds(long tenantId) {
		List<Long> ids = new ArrayList<>();
		ids.add(tenantId);
		for (Long library : libraryTenantIds()) {
			if (library != tenantId) {
				ids.add(library);
			}
		}
		return List.copyOf(ids);
	}

	/** Whether the current work may change content of {@code tenantId}. */
	public boolean canWrite(Long tenantId) {
		if (tenantId == null) {
			return false;
		}
		if (tenantView.isViewingOtherTenant()) {
			// An admin looking at someone else's library only reads (ADR013).
			return false;
		}
		if (tenantId == executionContext.tenantId()) {
			return true;
		}
		return isLibrary(tenantId) && adminProvider.isApplicationAdmin();
	}

	/**
	 * @throws ReadOnlyContentException when the current work may read but not change content of {@code tenantId}
	 */
	public void requireWritable(Long tenantId, String what) {
		if (!canWrite(tenantId)) {
			throw new ReadOnlyContentException(what + " belongs to the shared library and cannot be changed.");
		}
	}

	public boolean isLibrary(Long tenantId) {
		return tenantId != null && libraryTenantIds().contains(tenantId);
	}

	private List<Long> libraryTenantIds() {
		List<Long> ids = libraryTenantIds;
		if (ids == null) {
			ids = List.copyOf(tenantRepository.findIdsByType(TenantType.LIBRARY));
			libraryTenantIds = ids;
		}
		return ids;
	}
}
