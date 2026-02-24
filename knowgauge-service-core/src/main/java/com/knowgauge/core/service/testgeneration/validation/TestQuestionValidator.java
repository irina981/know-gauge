package com.knowgauge.core.service.testgeneration.validation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.AnswerOption;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates and normalizes LLM-generated TestQuestion objects
 * (POST-GENERATION).
 *
 * Responsibilities: - Ensure structural validity of generated questions
 * (question text, options, correct answers) - Normalize text fields - Optional
 * explanation enforcement - Optional grounding enforcement (source chunk ids) -
 * Remove duplicates within a generation batch (order-independent of options) -
 * Normalize questionIndex to sequential values - Enforce test.questionCount
 * limit - Optional enforcement of multi-correct quota for the validated batch
 *
 * Thread-safe and stateless.
 */
@Slf4j
@Component
public class TestQuestionValidator {

	// --- sizing / sanity guards (tune as needed) ---
	private static final int MIN_QUESTION_LEN = 12;
	private static final int MAX_QUESTION_LEN = 240;
	private static final int MAX_OPTION_LEN = 140;
	private static final int MAX_EXPLANATION_LEN = 500;

	// Reject multi-correct questions that reveal the answer count ("Which
	// two/three/4 ...")
	private static final Pattern COUNT_REVEALING = Pattern
			.compile("(?i)\\b(which|select|choose)\\s+(two|three|four|\\d+)\\b");

	// Heuristic: detect "mega options" that bundle multiple facts into one option.
	private static final int BUNDLE_COMMA_THRESHOLD = 3; // >= 2 commas/semicolons => likely bundle
	private static final int BUNDLE_WORD_THRESHOLD = 14; // long option + conjunction => suspicious

	private final boolean requireAllFourOptions;
	private final boolean requireExplanation;
	private final boolean requireGrounding;

	public TestQuestionValidator() {
		this(true, false, false);
	}

	public TestQuestionValidator(boolean requireAllFourOptions, boolean requireExplanation, boolean requireGrounding) {
		this.requireAllFourOptions = requireAllFourOptions;
		this.requireExplanation = requireExplanation;
		this.requireGrounding = requireGrounding;
	}

	/**
	 * Validates and normalizes questions. I case of test with "multiple-correct"
	 * answers cardinality, enforces multi-correct quota for this batch.
	 *
	 */
	public List<TestQuestion> validateAndNormalize(List<TestQuestion> questions, Test test,
			List<ChunkEmbedding> embeddings, int generatedCount) {
		if (test == null || questions == null || questions.isEmpty()) {
			log.warn("No questions to validate (null/empty).");
			return List.of();
		}

		int limit = (test != null && test.getQuestionCount() != null && test.getQuestionCount() > 0)
				? test.getQuestionCount()
				: Integer.MAX_VALUE;

		Set<Long> availableChunkIds = buildAvailableChunkIds(embeddings);

		List<TestQuestion> out = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		int nextIndex = generatedCount;

		for (int i = 0; i < questions.size(); i++) {

			TestQuestion q = questions.get(i);

			if (q == null) {
				log.warn("Rejected question at index {}: reason='question is null'", i);
				continue;
			}

			normalizeFields(q);

			String qPreview = preview(q.getQuestionText());

			// structure
			if (!isValidStructure(q, i)) {
				// isValidStructure logs the reason
				continue;
			}

			// grounding
			if (!validateGrounding(q, availableChunkIds, i)) {
				log.warn("Rejected question at index {}: reason='grounding validation failed', question='{}'", i,
						qPreview);
				continue;
			}

			// mega/bundled options
			String bundlingReason = bundledReason(q);
			if (bundlingReason != null) {
				log.warn("Rejected question at index {}: reason='{}', question='{}'", i, bundlingReason, qPreview);
				continue;
			}

			// count-revealing wording for multi-correct
			if (isCountRevealingAndMultiCorrect(q)) {
				log.warn(
						"Rejected question at index {}: reason='count-revealing wording in multi-correct question', question='{}'",
						i, qPreview);
				continue;
			}

			// dedupe (order-independent options)
			String key = dedupeKeyOrderIndependent(q);
			if (!seen.add(key)) {
				log.warn("Rejected question at index {}: reason='duplicate question in batch', question='{}'", i,
						qPreview);
				continue;
			}

			// shuffle + remap correct options
			shuffleOptionsAndRemapCorrectOptions(q);
			normalizeCorrectOptions(q);

			// post-shuffle sanity check
			if (!postShuffleSanity(q, i)) {
				// postShuffleSanity logs the reason
				continue;
			}

			q.setQuestionIndex(nextIndex++);
			out.add(q);

			if (out.size() >= limit) {
				break;
			}
		}

		// Enforce multi-correct quota (optional)
		if (AnswerCardinality.MULTIPLE_CORRECT.equals(test.getAnswerCardinality())) {
			Integer minMultipleCorrectQuestionsCount = test.getMinMultipleCorrectQuestionsCount();
			if (!enforceMultiCorrectQuota(out, minMultipleCorrectQuestionsCount)) {
				log.warn(
						"Rejected validated batch: reason='multi-correct quota not met', requiredMultiCorrect={}, returnedCount={}",
						minMultipleCorrectQuestionsCount, out.size());
				return List.of();
			}
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

		normalizeCorrectOptions(q);
	}

	private void normalizeCorrectOptions(TestQuestion q) {
		if (q.getCorrectOptions() == null)
			return;

		// distinct + stable order A,B,C,D
		EnumSet<AnswerOption> set = EnumSet.noneOf(AnswerOption.class);
		for (AnswerOption o : q.getCorrectOptions()) {
			if (o != null)
				set.add(o);
		}
		q.setCorrectOptions(new ArrayList<>(set));
	}

	// ---------------- structure validation ----------------

	private boolean isValidStructure(TestQuestion q, int index) {

		String qPreview = preview(q.getQuestionText());

		if (isBlank(q.getQuestionText())) {
			log.warn("Rejected question at index {}: reason='blank question text'", index);
			return false;
		}

		int qLen = q.getQuestionText().length();
		if (qLen < MIN_QUESTION_LEN || qLen > MAX_QUESTION_LEN) {
			log.warn("Rejected question at index {}: reason='question length out of bounds (len={})', question='{}'",
					index, qLen, qPreview);
			return false;
		}

		if (isBlank(q.getOptionA()) || isBlank(q.getOptionB())) {
			log.warn("Rejected question at index {}: reason='missing mandatory options A/B', question='{}'", index,
					qPreview);
			return false;
		}

		if (requireAllFourOptions) {
			if (isBlank(q.getOptionC()) || isBlank(q.getOptionD())) {
				log.warn("Rejected question at index {}: reason='missing required options C/D', question='{}'", index,
						qPreview);
				return false;
			}
		}

		if (tooLong(q.getOptionA(), MAX_OPTION_LEN) || tooLong(q.getOptionB(), MAX_OPTION_LEN)
				|| tooLong(q.getOptionC(), MAX_OPTION_LEN) || tooLong(q.getOptionD(), MAX_OPTION_LEN)) {
			log.warn("Rejected question at index {}: reason='option too long (max={})', question='{}'", index,
					MAX_OPTION_LEN, qPreview);
			return false;
		}

		if (q.getCorrectOptions() == null || q.getCorrectOptions().isEmpty()) {
			log.warn("Rejected question at index {}: reason='no correct options provided', question='{}'", index,
					qPreview);
			return false;
		}

		for (AnswerOption correctOption : q.getCorrectOptions()) {
			if (correctOption == null || isBlank(optionText(q, correctOption))) {
				log.warn("Rejected question at index {}: reason='correct option points to blank text', question='{}'",
						index, qPreview);
				return false;
			}
		}

		if (requireExplanation) {
			if (isBlank(q.getExplanation())) {
				log.warn("Rejected question at index {}: reason='missing explanation', question='{}'", index, qPreview);
				return false;
			}
		}

		if (!isBlank(q.getExplanation()) && q.getExplanation().length() > MAX_EXPLANATION_LEN) {
			log.warn("Rejected question at index {}: reason='explanation too long (len={}, max={})', question='{}'",
					index, q.getExplanation().length(), MAX_EXPLANATION_LEN, qPreview);
			return false;
		}

		if (hasDuplicateOptions(q)) {
			log.warn("Rejected question at index {}: reason='duplicate options detected', question='{}'", index,
					qPreview);
			return false;
		}

		if (hasAllOrNoneOption(q)) {
			log.warn("Rejected question at index {}: reason='contains All/None of the above option', question='{}'",
					index, qPreview);
			return false;
		}

		return true;
	}

	private boolean postShuffleSanity(TestQuestion q, int index) {
		String qPreview = preview(q.getQuestionText());

		if (requireAllFourOptions) {
			if (isBlank(q.getOptionA()) || isBlank(q.getOptionB()) || isBlank(q.getOptionC())
					|| isBlank(q.getOptionD())) {
				log.warn("Rejected question at index {}: reason='post-shuffle missing options', question='{}'", index,
						qPreview);
				return false;
			}
		}

		if (q.getCorrectOptions() == null || q.getCorrectOptions().isEmpty()) {
			log.warn("Rejected question at index {}: reason='post-shuffle missing correct options', question='{}'",
					index, qPreview);
			return false;
		}

		for (AnswerOption correctOption : q.getCorrectOptions()) {
			if (correctOption == null || isBlank(optionText(q, correctOption))) {
				log.warn(
						"Rejected question at index {}: reason='post-shuffle correct option points to blank text', question='{}'",
						index, qPreview);
				return false;
			}
		}

		if (hasDuplicateOptions(q)) {
			log.warn("Rejected question at index {}: reason='post-shuffle duplicate options detected', question='{}'",
					index, qPreview);
			return false;
		}

		String bundlingReason = bundledReason(q);
		if (bundlingReason != null) {
			log.warn("Rejected question at index {}: reason='post-shuffle {}', question='{}'", index, bundlingReason,
					qPreview);
			return false;
		}

		if (isCountRevealingAndMultiCorrect(q)) {
			log.warn(
					"Rejected question at index {}: reason='post-shuffle count-revealing wording in multi-correct question', question='{}'",
					index, qPreview);
			return false;
		}

		return true;
	}

	// ---------------- grounding validation ----------------

	private boolean validateGrounding(TestQuestion q, Set<Long> availableChunkIds, int index) {

		// Numeric chunk IDs (your existing model)
		if (q.getSourceChunkIdsJson() == null) {
			if (requireGrounding) {
				log.warn(
						"Rejected question at index {}: reason='missing sourceChunkIdsJson while grounding required', question='{}'",
						index, preview(q.getQuestionText()));
				return false;
			}
			return true;
		}

		List<Long> cleaned = q.getSourceChunkIdsJson().stream().filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());

		if (!availableChunkIds.isEmpty()) {
			cleaned = cleaned.stream().filter(availableChunkIds::contains).toList();
		}

		q.setSourceChunkIdsJson(cleaned);

		if (requireGrounding && cleaned.isEmpty()) {
			log.warn("Rejected question at index {}: reason='no valid source chunk ids after filtering', question='{}'",
					index, preview(q.getQuestionText()));
			return false;
		}

		return true;
	}

	// ---------------- multi-correct quota enforcement ----------------

	/**
	 * Enforces EXACTLY requiredMultiCorrect multi-correct questions (2–4 correct
	 * options). If not enough multi-correct exist, return false. If more exist, we
	 * keep the first requiredMultiCorrect (preserving existing order), and fill the
	 * rest with single-correct questions.
	 */
	private boolean enforceMultiCorrectQuota(List<TestQuestion> out, int requiredMultiCorrect) {

		if (requiredMultiCorrect < 0)
			return true;
		if (out.isEmpty())
			return requiredMultiCorrect == 0;

		List<TestQuestion> multi = out.stream()
				.filter(q -> q.getCorrectOptions() != null && q.getCorrectOptions().size() >= 2).toList();

		if (multi.size() < requiredMultiCorrect) {
			log.warn("Multi-correct quota not met: required={}, found={}", requiredMultiCorrect, multi.size());
			return false;
		}

		List<TestQuestion> single = out.stream()
				.filter(q -> q.getCorrectOptions() != null && q.getCorrectOptions().size() == 1).toList();

		// Keep count stable (= current out size)
		int total = out.size();

		List<TestQuestion> filtered = new ArrayList<>();
		filtered.addAll(multi.subList(0, requiredMultiCorrect));

		int remaining = total - filtered.size();
		if (remaining > 0) {
			filtered.addAll(single.subList(0, Math.min(remaining, single.size())));
		}

		// Re-index sequentially based on first index (if any)
		int baseIndex = filtered.get(0).getQuestionIndex();
		for (int i = 0; i < filtered.size(); i++) {
			filtered.get(i).setQuestionIndex(baseIndex + i);
		}

		out.clear();
		out.addAll(filtered);

		return true;
	}

	// ---------------- duplicates ----------------

	/**
	 * Dedupe independent of option order.
	 */
	private String dedupeKeyOrderIndependent(TestQuestion q) {

		String qKey = normKey(q.getQuestionText());

		List<String> opts = List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()).stream()
				.filter(s -> !isBlank(s)).map(this::normKey).sorted().toList();

		return qKey + "||" + String.join("|", opts);
	}

	private boolean hasDuplicateOptions(TestQuestion q) {

		List<String> opts = List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());

		List<String> normalized = opts.stream().filter(s -> !isBlank(s)).map(this::normKey).toList();

		return normalized.size() != new HashSet<>(normalized).size();
	}

	// ---------------- bundled options detection ----------------

	private String bundledReason(TestQuestion q) {
		if (isBundled(q.getOptionA()))
			return "bundled/mega option detected in A";
		if (isBundled(q.getOptionB()))
			return "bundled/mega option detected in B";
		if (isBundled(q.getOptionC()))
			return "bundled/mega option detected in C";
		if (isBundled(q.getOptionD()))
			return "bundled/mega option detected in D";
		return null;
	}

	private boolean isBundled(String option) {
		if (isBlank(option))
			return false;

		String t = option.trim();

		int separators = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == ',' || c == ';')
				separators++;
		}

		int words = t.split("\\s+").length;
		if (separators >= BUNDLE_COMMA_THRESHOLD && words >= BUNDLE_WORD_THRESHOLD) {
			String lower = t.toLowerCase(Locale.ROOT);
			if (lower.contains(" and ") || lower.contains(" or ") || lower.contains(" as well as "))
				return true;
		}

		return false;
	}

	private boolean hasAllOrNoneOption(TestQuestion q) {
		return isAllOrNone(q.getOptionA()) || isAllOrNone(q.getOptionB()) || isAllOrNone(q.getOptionC())
				|| isAllOrNone(q.getOptionD());
	}

	private boolean isAllOrNone(String s) {
		if (isBlank(s))
			return false;
		String t = normKey(s);
		return t.equals("all of the above") || t.equals("none of the above") || t.equals("all of these")
				|| t.equals("none of these");
	}

	private boolean isCountRevealingAndMultiCorrect(TestQuestion q) {
		int correctCount = (q.getCorrectOptions() == null) ? 0 : q.getCorrectOptions().size();
		if (correctCount < 2)
			return false;
		return COUNT_REVEALING.matcher(q.getQuestionText()).find();
	}

	// ---------------- shuffle & remap ----------------

	private void shuffleOptionsAndRemapCorrectOptions(TestQuestion q) {

		if (isBlank(q.getOptionA()) || isBlank(q.getOptionB()) || isBlank(q.getOptionC()) || isBlank(q.getOptionD())) {
			return;
		}

		List<AnswerOption> originalCorrectOptions = q.getCorrectOptions();
		if (originalCorrectOptions == null || originalCorrectOptions.isEmpty()) {
			return;
		}

		Set<String> originalCorrectTexts = originalCorrectOptions.stream().map(option -> optionText(q, option))
				.filter(Objects::nonNull).map(this::normalizeText).collect(Collectors.toSet());

		if (originalCorrectTexts.isEmpty()) {
			return;
		}

		List<String> shuffledOptions = new ArrayList<>(
				List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));

		Collections.shuffle(shuffledOptions, ThreadLocalRandom.current());

		q.setOptionA(shuffledOptions.get(0));
		q.setOptionB(shuffledOptions.get(1));
		q.setOptionC(shuffledOptions.get(2));
		q.setOptionD(shuffledOptions.get(3));

		List<AnswerOption> remappedCorrectOptions = new ArrayList<>();

		if (originalCorrectTexts.contains(normalizeText(q.getOptionA())))
			remappedCorrectOptions.add(AnswerOption.A);
		if (originalCorrectTexts.contains(normalizeText(q.getOptionB())))
			remappedCorrectOptions.add(AnswerOption.B);
		if (originalCorrectTexts.contains(normalizeText(q.getOptionC())))
			remappedCorrectOptions.add(AnswerOption.C);
		if (originalCorrectTexts.contains(normalizeText(q.getOptionD())))
			remappedCorrectOptions.add(AnswerOption.D);

		q.setCorrectOptions(remappedCorrectOptions);
	}

	private String optionText(TestQuestion q, AnswerOption opt) {
		return switch (opt) {
		case A -> q.getOptionA();
		case B -> q.getOptionB();
		case C -> q.getOptionC();
		case D -> q.getOptionD();
		};
	}

	// ---------------- text normalization ----------------

	private String normalizeText(String s) {

		if (s == null)
			return null;

		String t = Normalizer.normalize(s, Normalizer.Form.NFKC);

		t = t.trim().replaceAll("\\s+", " ");

		t = t.replaceAll("^(Q\\d+\\s*[:.)]\\s*)", "");
		t = t.replaceAll("^(Question\\s*[:.)]\\s*)", "");

		return t.trim();
	}

	private String normKey(String s) {
		String n = normalizeText(s);
		return n == null ? "" : n.toLowerCase(Locale.ROOT);
	}

	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private boolean tooLong(String s, int max) {
		return s != null && s.length() > max;
	}

	private String preview(String s) {
		if (s == null)
			return "null";
		String t = s.replaceAll("\\s+", " ").trim();
		return t.length() > 140 ? t.substring(0, 140) + "..." : t;
	}

	// ---------------- chunk ids helper ----------------

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