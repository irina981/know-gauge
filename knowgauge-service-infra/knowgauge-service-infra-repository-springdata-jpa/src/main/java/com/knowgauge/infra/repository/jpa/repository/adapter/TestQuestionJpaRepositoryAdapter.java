package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.QuestionStatus;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionEntity;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionVersionEntity;
import com.knowgauge.infra.repository.jpa.mapper.TestQuestionEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TestQuestionJpaRepository;
import com.knowgauge.infra.repository.jpa.repository.TestQuestionVersionJpaRepository;

@Repository
public class TestQuestionJpaRepositoryAdapter implements TestQuestionRepository {

	private final TestQuestionJpaRepository questionRepo;
	private final TestQuestionVersionJpaRepository versionRepo;
	private final TestQuestionEntityMapper mapper;

	public TestQuestionJpaRepositoryAdapter(TestQuestionJpaRepository questionRepo,
			TestQuestionVersionJpaRepository versionRepo, TestQuestionEntityMapper mapper) {
		this.questionRepo = questionRepo;
		this.versionRepo = versionRepo;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public List<TestQuestion> saveNewWithInitialVersions(List<TestQuestion> questions) {
		List<TestQuestion> result = new ArrayList<>(questions.size());
		for (TestQuestion q : questions) {
			if (q.getCurrentVersion() == null) {
				throw new IllegalArgumentException("Question has no content (current version) to save");
			}
			// 1) slot without current version (FK cycle: slot <-> version)
			TestQuestionEntity slot = mapper.toEntity(q);
			slot.setId(null);
			slot.setCurrentVersionId(null);
			slot = questionRepo.save(slot);

			// 2) version 1 pointing at the slot
			TestQuestionVersion v = q.getCurrentVersion();
			v.setQuestionId(slot.getId());
			TestQuestionVersionEntity savedVersion = versionRepo.save(mapper.toEntity(v));

			// 3) slot -> version (managed entity, flushed on commit)
			slot.setCurrentVersionId(savedVersion.getId());
			result.add(mapper.toDomain(slot, savedVersion));
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<TestQuestion> findByTenantIdAndId(Long tenantId, Long id) {
		return questionRepo.findByTenantIdAndId(tenantId, id).map(slot -> mapper.toDomain(slot,
				slot.getCurrentVersionId() != null ? versionRepo.findById(slot.getCurrentVersionId()).orElse(null)
						: null));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TestQuestion> findByTenantIdAndTestId(Long tenantId, Long testId) {
		List<TestQuestionEntity> slots = questionRepo.findByTenantIdAndTestIdOrderByQuestionIndexAsc(tenantId, testId);
		List<Long> versionIds = slots.stream().map(TestQuestionEntity::getCurrentVersionId).filter(Objects::nonNull)
				.toList();
		Map<Long, TestQuestionVersionEntity> versions = versionIds.isEmpty() ? Map.of()
				: versionRepo.findByTenantIdAndIdIn(tenantId, versionIds).stream()
						.collect(Collectors.toMap(TestQuestionVersionEntity::getId, Function.identity()));
		return slots.stream().map(slot -> mapper.toDomain(slot, versions.get(slot.getCurrentVersionId()))).toList();
	}

	@Override
	@Transactional
	public TestQuestionVersion insertVersion(TestQuestionVersion version) {
		if (version.getId() != null) {
			throw new IllegalArgumentException("Versions are immutable; insert a new version instead of updating "
					+ version.getId());
		}
		return mapper.toDomain(versionRepo.save(mapper.toEntity(version)));
	}

	@Override
	@Transactional
	public boolean switchCurrentVersion(Long tenantId, Long questionId, Long expectedCurrentVersionId,
			Long newVersionId, QuestionStatus newStatus) {
		// TODO: replace the hard-coded actor once Spring Data auditing is wired (see AuditableEntity)
		return questionRepo.switchCurrentVersion(tenantId, questionId, expectedCurrentVersionId, newVersionId,
				newStatus, 1L, Instant.now()) == 1;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TestQuestionVersion> findVersions(Long tenantId, Long questionId) {
		return versionRepo.findByTenantIdAndQuestionIdOrderByVersionNoAsc(tenantId, questionId).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public int findMaxVersionNo(Long tenantId, Long questionId) {
		return versionRepo.findMaxVersionNo(tenantId, questionId);
	}
}
