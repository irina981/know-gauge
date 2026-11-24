package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.infra.repository.jpa.mapper.TenantEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TenantJpaRepository;

@Repository
public class TenantJpaRepositoryAdapter implements TenantRepository {

	private static final String FROM = " from TenantEntity t left join UserEntity u on u.id = t.ownerUserId";
	private static final String TOPICS = "(select count(tp) from TopicEntity tp where tp.tenantId = t.id)";
	private static final String DOCUMENTS = "(select count(d) from DocumentEntity d where d.tenantId = t.id)";
	/** Workspace invitations of the tenant (ADR015), accepted or not. */
	private static final String INVITATIONS = "(select count(wi) from WorkspaceInvitationEntity wi where wi.tenantId = t.id)";
	/** Total size of the tenant's documents in bytes. */
	private static final String STORAGE = "(select coalesce(sum(ds.fileSizeBytes), 0L) from DocumentEntity ds where ds.tenantId = t.id)";
	/** LLM tokens (input + output) of the tenant's calls in the usage period. */
	private static final String TOKENS = "(select coalesce(sum(coalesce(r.inputTokens, 0) + coalesce(r.outputTokens, 0)), 0L)"
			+ " from GenerationRunEntity r where r.tenantId = t.id and r.createdAt >= :usageFrom and r.createdAt < :usageTo)";
	private static final String SELECT = "select new com.knowgauge.core.model.TenantSummary(t.id, t.name, t.type,"
			+ " t.ownerUserId, u.displayName, u.email, t.createdAt, " + TOPICS + ", " + DOCUMENTS + ", " + INVITATIONS + ", " + STORAGE + ", " + TOKENS + ")";

	@PersistenceContext
	private EntityManager em;

	private final TenantJpaRepository jpaRepository;
	private final TenantEntityMapper mapper;

	public TenantJpaRepositoryAdapter(TenantJpaRepository jpaRepository, TenantEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Tenant save(Tenant tenant) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(tenant)));
	}

	@Override
	public Optional<Tenant> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Long> findIdsByType(TenantType type) {
		return jpaRepository.findIdsByType(type);
	}

	/** Sortable columns of the admin list and their JPQL expressions. */
	private static final Map<String, String> SORT_EXPRESSIONS = Map.of(
			"name", "lower(t.name)",
			"type", "t.type",
			"owner", "lower(coalesce(u.displayName, u.email))",
			"topics", TOPICS,
			"documents", DOCUMENTS,
			"storage", STORAGE,
			"usage", TOKENS,
			"createdAt", "t.createdAt");

	@Override
	public Page<TenantSummary> findTenantSummaries(TenantSearch search, Pageable pageable, Instant usageFrom,
			Instant usageTo) {
		StringBuilder where = new StringBuilder(" where 1 = 1");
		Map<String, Object> params = new HashMap<>();
		if (search != null && search.text() != null && !search.text().isBlank()) {
			where.append(" and (lower(t.name) like :text or lower(u.displayName) like :text or lower(u.email) like :text)");
			params.put("text", "%" + search.text().trim().toLowerCase(Locale.ROOT) + "%");
		}
		if (search != null && search.type() != null) {
			where.append(" and t.type = :type");
			params.put("type", search.type());
		}

		StringBuilder order = new StringBuilder();
		for (Sort.Order o : pageable.getSort()) {
			String expression = SORT_EXPRESSIONS.get(o.getProperty());
			if (expression == null) {
				// TenantAdminServiceImpl checks the property first (TenantSearch.SORTABLE).
				throw new IllegalArgumentException("Cannot sort tenants by '" + o.getProperty() + "'");
			}
			order.append(order.isEmpty() ? " order by " : ", ").append(expression).append(o.isAscending() ? " asc" : " desc");
		}
		// A stable order, so pages never repeat or skip a tenant.
		order.append(order.isEmpty() ? " order by " : ", ").append("t.id asc");

		TypedQuery<TenantSummary> query = em.createQuery(SELECT + FROM + where + order, TenantSummary.class);
		TypedQuery<Long> count = em.createQuery("select count(t)" + FROM + where, Long.class);
		params.forEach((name, value) -> {
			query.setParameter(name, value);
			count.setParameter(name, value);
		});
		// Only the list selects (and may sort by) the tokens; the count does not.
		query.setParameter("usageFrom", usageFrom).setParameter("usageTo", usageTo);
		if (pageable.isPaged()) {
			query.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
		}
		return new PageImpl<>(query.getResultList(), pageable, count.getSingleResult());
	}

	@Override
	public Optional<TenantSummary> findTenantSummary(Long tenantId, Instant usageFrom, Instant usageTo) {
		return em.createQuery(SELECT + FROM + " where t.id = :id", TenantSummary.class).setParameter("id", tenantId)
				.setParameter("usageFrom", usageFrom).setParameter("usageTo", usageTo).getResultStream().findFirst();
	}
}
