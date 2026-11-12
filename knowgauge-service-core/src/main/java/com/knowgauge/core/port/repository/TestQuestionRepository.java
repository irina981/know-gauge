package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.QuestionStatus;

/**
 * Persistence of question slots and their immutable versions (see ADR001).
 * Returned questions always have their current version loaded.
 */
public interface TestQuestionRepository {

	/**
	 * Inserts new question slots together with their draft current version (which
	 * becomes version 1), and points each slot at it.
	 *
	 * @return the saved questions with ids, version ids and current versions set
	 */
	List<TestQuestion> saveNewWithInitialVersions(List<TestQuestion> questions);

	Optional<TestQuestion> findByTenantIdAndId(Long tenantId, Long id);

	/**
	 * @return questions of the test ordered by question index
	 */
	List<TestQuestion> findByTenantIdAndTestId(Long tenantId, Long testId);

	/**
	 * Inserts a new (immutable) version.
	 */
	TestQuestionVersion insertVersion(TestQuestionVersion version);

	/**
	 * Atomically moves the slot to a new current version, if the slot still points
	 * to {@code expectedCurrentVersionId} (optimistic concurrency).
	 *
	 * @return true when switched, false when another change got there first
	 */
	boolean switchCurrentVersion(Long tenantId, Long questionId, Long expectedCurrentVersionId, Long newVersionId,
			QuestionStatus newStatus);

	/**
	 * @return all versions of a question ordered by version number
	 */
	List<TestQuestionVersion> findVersions(Long tenantId, Long questionId);

	/**
	 * @return highest version number of the question, 0 if none
	 */
	int findMaxVersionNo(Long tenantId, Long questionId);
}
