package com.knowgauge.infra.repository.jpa.mapper;

import java.util.ArrayList;
import java.util.EnumMap;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionEntity;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionVersionEntity;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionVersionOptionEntity;

/**
 * Maps question slots and versions between domain and JPA (hand-written: the
 * domain question combines a slot with its separately stored current version).
 */
@Component
public class TestQuestionEntityMapper {

	// ---------------- slot ----------------

	public TestQuestionEntity toEntity(TestQuestion domain) {
		return TestQuestionEntity.builder().id(domain.getId()).tenantId(domain.getTenantId())
				.testId(domain.getTestId()).questionIndex(domain.getQuestionIndex()).status(domain.getStatus())
				.currentVersionId(domain.getCurrentVersionId()).build();
	}

	public TestQuestion toDomain(TestQuestionEntity entity, TestQuestionVersionEntity currentVersion) {
		TestQuestion q = TestQuestion.builder().id(entity.getId()).tenantId(entity.getTenantId())
				.testId(entity.getTestId()).questionIndex(entity.getQuestionIndex()).status(entity.getStatus())
				.currentVersionId(entity.getCurrentVersionId())
				.currentVersion(currentVersion != null ? toDomain(currentVersion) : null).build();
		q.setCreatedAt(entity.getCreatedAt());
		q.setCreatedBy(entity.getCreatedBy());
		q.setUpdatedAt(entity.getUpdatedAt());
		q.setUpdatedBy(entity.getUpdatedBy());
		return q;
	}

	// ---------------- version ----------------

	public TestQuestionVersionEntity toEntity(TestQuestionVersion domain) {
		TestQuestionVersionEntity entity = TestQuestionVersionEntity.builder().id(domain.getId())
				.tenantId(domain.getTenantId()).questionId(domain.getQuestionId()).versionNo(domain.getVersionNo())
				.origin(domain.getOrigin()).parentVersionId(domain.getParentVersionId())
				.generationRunId(domain.getGenerationRunId()).verificationRunId(domain.getVerificationRunId())
				.questionText(domain.getQuestionText()).explanation(domain.getExplanation())
				.sourceChunkIds(domain.getSourceChunkIds() != null ? new ArrayList<>(domain.getSourceChunkIds()) : null)
				.createdAt(domain.getCreatedAt()).createdBy(domain.getCreatedBy()).build();
		for (TestQuestionOption option : domain.getOptions().values()) {
			if (option == null) {
				continue;
			}
			entity.getOptions().add(TestQuestionVersionOptionEntity.builder().version(entity)
					.letter(option.getLetter()).text(option.getText()).correct(option.isCorrect())
					.flagged(option.isFlagged()).suggestedText(option.getSuggestedText())
					.flagReason(option.getFlagReason()).build());
		}
		return entity;
	}

	public TestQuestionVersion toDomain(TestQuestionVersionEntity entity) {
		TestQuestionVersion v = TestQuestionVersion.builder().id(entity.getId()).tenantId(entity.getTenantId())
				.questionId(entity.getQuestionId()).versionNo(entity.getVersionNo()).origin(entity.getOrigin())
				.parentVersionId(entity.getParentVersionId()).generationRunId(entity.getGenerationRunId())
				.verificationRunId(entity.getVerificationRunId()).questionText(entity.getQuestionText())
				.explanation(entity.getExplanation())
				.sourceChunkIds(entity.getSourceChunkIds() != null ? new ArrayList<>(entity.getSourceChunkIds()) : null)
				.createdAt(entity.getCreatedAt()).createdBy(entity.getCreatedBy()).build();
		EnumMap<OptionLetter, TestQuestionOption> options = new EnumMap<>(OptionLetter.class);
		for (TestQuestionVersionOptionEntity o : entity.getOptions()) {
			options.put(o.getLetter(), TestQuestionOption.builder().letter(o.getLetter()).text(o.getText())
					.correct(o.isCorrect()).flagged(o.isFlagged()).suggestedText(o.getSuggestedText())
					.flagReason(o.getFlagReason()).build());
		}
		v.setOptions(options);
		return v;
	}
}
