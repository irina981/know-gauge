package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.time.Instant;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.UsageSummary;
import com.knowgauge.core.port.repository.UsageRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Aggregates over the main database; every query filters by tenant. */
@Repository
public class UsageJpaRepositoryAdapter implements UsageRepository {

	@PersistenceContext
	private EntityManager em;

	@Override
	public UsageSummary summarize(Long tenantId, Instant from, Instant to) {
		Object[] content = (Object[]) em.createQuery("""
				SELECT COUNT(d), COALESCE(SUM(CASE WHEN d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTED
				THEN 1 ELSE 0 END), 0), COALESCE(SUM(d.fileSizeBytes), 0)
				FROM DocumentEntity d WHERE d.tenantId = :tenantId""").setParameter("tenantId", tenantId)
				.getSingleResult();
		long topics = count("SELECT COUNT(t) FROM TopicEntity t WHERE t.tenantId = :tenantId", tenantId, null, null);
		long tests = count("SELECT COUNT(t) FROM TestEntity t WHERE t.tenantId = :tenantId"
				+ " AND t.createdAt >= :from AND t.createdAt < :to", tenantId, from, to);
		// LLM-written versions: version 1 of every generated question and every regeneration.
		long questions = count("SELECT COUNT(v) FROM TestQuestionVersionEntity v WHERE v.tenantId = :tenantId"
				+ " AND v.origin IN (com.knowgauge.core.model.enums.VersionOrigin.GENERATED,"
				+ " com.knowgauge.core.model.enums.VersionOrigin.REGENERATED)"
				+ " AND v.createdAt >= :from AND v.createdAt < :to", tenantId, from, to);
		long attempts = count("SELECT COUNT(a) FROM AttemptEntity a WHERE a.tenantId = :tenantId"
				+ " AND a.submittedAt >= :from AND a.submittedAt < :to", tenantId, from, to);
		Object[] tokens = (Object[]) em.createQuery("""
				SELECT COALESCE(SUM(r.inputTokens), 0), COALESCE(SUM(r.outputTokens), 0)
				FROM GenerationRunEntity r
				WHERE r.tenantId = :tenantId AND r.createdAt >= :from AND r.createdAt < :to""")
				.setParameter("tenantId", tenantId).setParameter("from", from).setParameter("to", to)
				.getSingleResult();
		return UsageSummary.builder().periodStart(from).periodEnd(to).topics(topics)
				.documents(toLong(content[0])).ingestedDocuments(toLong(content[1])).storageBytes(toLong(content[2]))
				.tests(tests).questions(questions).submittedAttempts(attempts).llmInputTokens(toLong(tokens[0]))
				.llmOutputTokens(toLong(tokens[1])).build();
	}

	private long count(String jpql, Long tenantId, Instant from, Instant to) {
		var query = em.createQuery(jpql, Long.class).setParameter("tenantId", tenantId);
		if (from != null) {
			query.setParameter("from", from).setParameter("to", to);
		}
		return query.getSingleResult();
	}

	private static long toLong(Object value) {
		return value == null ? 0 : ((Number) value).longValue();
	}
}
