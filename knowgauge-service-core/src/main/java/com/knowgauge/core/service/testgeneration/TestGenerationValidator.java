package com.knowgauge.core.service.testgeneration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerOption;

/**
 * Validates and normalizes LLM-generated TestQuestion objects.
 *
 * PURPOSE ------- This class acts as a safety gate between the LLM output and
 * persistence. LLMs are probabilistic and may produce malformed, duplicate, or
 * incomplete questions.
 *
 * This validator ensures that only structurally valid, normalized, and usable
 * questions are accepted and persisted.
 *
 * IMPORTANT DESIGN PRINCIPLE --------------------------- This validator DOES
 * NOT enforce retrieval logic such as "avoid previously used chunks". That
 * responsibility belongs to the chunk selection / retrieval stage.
 *
 * This validator focuses strictly on OUTPUT QUALITY and STRUCTURAL INTEGRITY.
 *
 * VALIDATION RULES ---------------- The validator performs the following checks
 * and normalization steps:
 *
 * 1. Question text validation - must not be null or blank - must not be too
 * short (prevents garbage output like "?", "Q1", etc.) - normalized (trim,
 * collapse whitespace, remove common LLM prefixes)
 *
 * 2. Option validation - Option A and B are mandatory - Option C and D are
 * mandatory if requireAllFourOptions=true - options are normalized (trim,
 * collapse whitespace) - duplicate options are not allowed (question is
 * dropped)
 *
 * 3. Correct answer validation - correctOption must be present - correctOption
 * must reference a non-empty option
 *
 * 4. Explanation validation (optional) - enforced only if
 * requireExplanation=true
 *
 * 5. Grounding metadata validation (optional safety net) - sourceChunkIdsJson
 * cleaned from nulls and duplicates - invalid chunkIds (not present in
 * embeddings) removed - grounding may be enforced via requireGrounding flag
 *
 * 6. Duplicate question prevention (within single generation batch) - prevents
 * identical questions generated multiple times
 *
 * 7. Question index normalization - questionIndex reassigned sequentially
 * (0..N) - ensures deterministic ordering regardless of LLM output order
 *
 * 8. Question count limit enforcement - respects test.questionCount constraint
 *
 *
 * SAFETY POLICY ------------- The validator NEVER attempts risky "auto-fixes"
 * such as: - shifting options A–D - guessing correct answers
 *
 * Instead, malformed questions are safely rejected.
 *
 *
 * CONFIGURATION FLAGS ------------------- requireAllFourOptions true -> strict
 * MCQ (A–D required) false -> allows fewer options (>= 2)
 *
 * requireExplanation true -> explanation required false -> explanation optional
 *
 * requireGrounding true -> sourceChunkIdsJson must be present and valid false
 * -> grounding optional
 *
 *
 * THREAD SAFETY ------------- Stateless and thread-safe.
 */
@Component
public class TestGenerationValidator {

	/** If true, enforce exactly 4 options (A–D). */
	private final boolean requireAllFourOptions;

	/** If true, explanation must be present. */
	private final boolean requireExplanation;

	/** If true, grounding metadata must be present and valid. */
	private final boolean requireGrounding;

	/**
	 * Default constructor with safe defaults for production use.
	 *
	 * requireAllFourOptions = true requireExplanation = false requireGrounding =
	 * false
	 */
	public TestGenerationValidator() {
		this(true, false, false);
	}

	/**
	 * Fully configurable constructor.
	 */
	public TestGenerationValidator(boolean requireAllFourOptions, boolean requireExplanation,
			boolean requireGrounding) {
		this.requireAllFourOptions = requireAllFourOptions;
		this.requireExplanation = requireExplanation;
		this.requireGrounding = requireGrounding;
	}

	/**
	 * Main validation entry point.
	 *
	 * @param questions  raw LLM-generated questions
	 * @param test       test configuration
	 * @param embeddings embeddings used for generation (used for grounding
	 *                   validation)
	 *
	 * @return validated and normalized question list
	 */
	public List<TestQuestion> validateAndNormalize(List<TestQuestion> questions, Test test,
			List<ChunkEmbedding> embeddings) {

		if (questions == null || questions.isEmpty()) {
			return List.of();
		}

		// Enforce test.questionCount limit
		int limit = (test != null && test.getQuestionCount() != null && test.getQuestionCount() > 0)
				? test.getQuestionCount()
				: Integer.MAX_VALUE;

		// Used for grounding validation safety net
		Set<Long> availableChunkIds = buildAvailableChunkIds(embeddings);

		List<TestQuestion> out = new ArrayList<>();

		// Used to prevent duplicate questions in same generation batch
		Set<String> seen = new HashSet<>();

		int nextIndex = 0;

		for (TestQuestion q : questions) {

			if (q == null)
				continue;

			// Normalize text fields
			normalizeFields(q);

			// Validate core question structure
			if (!isValidStructure(q))
				continue;

			// Validate grounding metadata
			if (!validateGrounding(q, availableChunkIds))
				continue;

			// Prevent duplicates
			String key = dedupeKey(q);
			if (!seen.add(key))
				continue;

			// Normalize question index
			q.setQuestionIndex(nextIndex++);

			out.add(q);

			// Respect question count limit
			if (out.size() >= limit)
				break;
		}

		return out;
	}

	// ---------------- normalization ----------------

	private void normalizeFields(TestQuestion q) {

		q.setQuestionText(normalizeText(q.getQuestionText()));

		q.setOptionA(normalizeText(q.getOptionA()));
		q.setOptionB(normalizeText(q.getOptionB()));
		q.setOptionC(normalizeText(q.getOptionC()));
		q.setOptionD(normalizeText(q.getOptionD()));

		q.setExplanation(normalizeText(q.getExplanation()));
	}

	// ---------------- structure validation ----------------

	private boolean isValidStructure(TestQuestion q) {

		if (isBlank(q.getQuestionText()))
			return false;

		// Prevent garbage questions
		if (q.getQuestionText().length() < 12)
			return false;

		// Option validation
		if (isBlank(q.getOptionA()) || isBlank(q.getOptionB()))
			return false;

		if (requireAllFourOptions) {
			if (isBlank(q.getOptionC()) || isBlank(q.getOptionD()))
				return false;
		}

		// Correct answer validation
		if (q.getCorrectOption() == null)
			return false;

		if (isBlank(optionText(q, q.getCorrectOption())))
			return false;

		// Explanation validation (optional)
		if (requireExplanation && isBlank(q.getExplanation()))
			return false;

		// Prevent duplicate options
		if (hasDuplicateOptions(q))
			return false;

		return true;
	}

	// ---------------- grounding validation ----------------

	private boolean validateGrounding(TestQuestion q, Set<Long> availableChunkIds) {

		if (q.getSourceChunkIdsJson() == null) {
			return !requireGrounding;
		}

		List<Long> cleaned = q.getSourceChunkIdsJson().stream().filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());

		// Safety net: remove invalid chunk IDs
		if (!availableChunkIds.isEmpty()) {
			cleaned = cleaned.stream().filter(availableChunkIds::contains).toList();
		}

		q.setSourceChunkIdsJson(cleaned);

		if (requireGrounding && cleaned.isEmpty())
			return false;

		return true;
	}

	// ---------------- duplicate prevention ----------------

	private String dedupeKey(TestQuestion q) {

		return normKey(q.getQuestionText()) + "||" + normKey(q.getOptionA()) + "|" + normKey(q.getOptionB()) + "|"
				+ normKey(q.getOptionC()) + "|" + normKey(q.getOptionD());
	}

	// ---------------- helpers ----------------

	private boolean hasDuplicateOptions(TestQuestion q) {

		List<String> opts = List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());

		List<String> normalized = opts.stream().filter(s -> !isBlank(s)).map(this::normKey).toList();

		return normalized.size() != new HashSet<>(normalized).size();
	}

	private String optionText(TestQuestion q, AnswerOption opt) {

		return switch (opt) {
		case A -> q.getOptionA();
		case B -> q.getOptionB();
		case C -> q.getOptionC();
		case D -> q.getOptionD();
		};
	}

	private String normalizeText(String s) {

		if (s == null)
			return null;

		String t = Normalizer.normalize(s, Normalizer.Form.NFKC);

		t = t.trim().replaceAll("\\s+", " ");

		// Remove common LLM prefixes
		t = t.replaceAll("^(Q\\d+\\s*[:.)]\\s*)", "");
		t = t.replaceAll("^(Question\\s*[:.)]\\s*)", "");

		return t.trim();
	}

	private String normKey(String s) {

		return normalizeText(s).toLowerCase(Locale.ROOT);
	}

	private boolean isBlank(String s) {

		return s == null || s.trim().isEmpty();
	}

	/**
	 * Extracts valid chunk IDs from embeddings.
	 *
	 * Used only as safety net validation.
	 */
	private Set<Long> buildAvailableChunkIds(List<ChunkEmbedding> embeddings) {

		if (embeddings == null || embeddings.isEmpty()) {
			return Set.of();
		}

		Set<Long> ids = new HashSet<>();

		for (ChunkEmbedding e : embeddings) {

			try {
				var m = e.getClass().getMethod("getChunkId");
				Object v = m.invoke(e);
				if (v instanceof Long l)
					ids.add(l);
			} catch (Exception ignored) {
			}
		}

		return ids;
	}
}
