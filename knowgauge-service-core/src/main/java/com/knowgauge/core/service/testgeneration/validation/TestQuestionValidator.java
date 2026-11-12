package com.knowgauge.core.service.testgeneration.validation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates and normalizes draft questions (LLM-generated or manually written).
 *
 * <p>
 * Each rule is a {@link ValidationStrategy}. The pipeline picks a strategy list
 * per stage: {@link #preLlmStrategies()} before verification,
 * {@link #postLlmStrategies()} after it, and {@link #manualStrategies()} for
 * reviewer-written questions. Correctness is always read from the answer key
 * ({@code option.correct}); verifier flags never affect validation.
 *
 * <p>
 * Thread-safe and stateless.
 */
@Slf4j
@Component
public class TestQuestionValidator {

	// --- sizing / sanity guards (tune as needed) ---
	static final int MIN_QUESTION_LEN = 12;
	static final int MAX_QUESTION_LEN = 240;
	static final int MAX_OPTION_LEN = 140;
	static final int MAX_EXPLANATION_LEN = 500;

	// Reject multi-correct questions that reveal the answer count ("Which
	// two/three/4 ...")
	private static final Pattern COUNT_REVEALING = Pattern
			.compile("(?i)\\b(which|select|choose)\\s+(two|three|four|\\d+)\\b");

	// Heuristic: detect "mega options" that bundle multiple facts into one option.
	private static final int BUNDLE_COMMA_THRESHOLD = 3;
	private static final int BUNDLE_WORD_THRESHOLD = 14;

	private final boolean requireAllFourOptions;
	private final boolean requireExplanation;
	private final boolean requireGrounding;

	public enum ValidationStrategy {
		NORMALIZE_FIELDS, STRUCTURE, GROUNDING, BUNDLED_OPTIONS, COUNT_REVEALING, DEDUPE, CORRECT_OPTIONS_CARDINALITY,
		SHUFFLE_AND_REMAP, POST_SHUFFLE_SANITY, MULTI_CORRECT_QUOTA
	}

	public TestQuestionValidator() {
		this(true, false, false);
	}

	public TestQuestionValidator(boolean requireAllFourOptions, boolean requireExplanation, boolean requireGrounding) {
		this.requireAllFourOptions = requireAllFourOptions;
		this.requireExplanation = requireExplanation;
		this.requireGrounding = requireGrounding;
	}

	public static List<ValidationStrategy> preLlmStrategies() {
		return List.of(ValidationStrategy.NORMALIZE_FIELDS, ValidationStrategy.STRUCTURE, ValidationStrategy.GROUNDING,
				ValidationStrategy.BUNDLED_OPTIONS, ValidationStrategy.COUNT_REVEALING, ValidationStrategy.DEDUPE,
				ValidationStrategy.CORRECT_OPTIONS_CARDINALITY);
	}

	public static List<ValidationStrategy> postLlmStrategies() {
		return List.of(ValidationStrategy.NORMALIZE_FIELDS, ValidationStrategy.STRUCTURE, ValidationStrategy.GROUNDING,
				ValidationStrategy.BUNDLED_OPTIONS, ValidationStrategy.COUNT_REVEALING, ValidationStrategy.DEDUPE,
				ValidationStrategy.CORRECT_OPTIONS_CARDINALITY, ValidationStrategy.SHUFFLE_AND_REMAP,
				ValidationStrategy.POST_SHUFFLE_SANITY, ValidationStrategy.MULTI_CORRECT_QUOTA);
	}

	/**
	 * Non-LLM rules for reviewer-written questions: no grounding, no shuffle (the
	 * reviewer chose the order), no batch quota.
	 */
	public static List<ValidationStrategy> manualStrategies() {
		return List.of(ValidationStrategy.NORMALIZE_FIELDS, ValidationStrategy.STRUCTURE,
				ValidationStrategy.BUNDLED_OPTIONS, ValidationStrategy.COUNT_REVEALING,
				ValidationStrategy.CORRECT_OPTIONS_CARDINALITY);
	}

	/**
	 * Validates a batch: rejected questions are dropped (and logged), valid ones
	 * are normalized and get sequential indexes from {@code startIndex}.
	 *
	 * @param availableChunkIds chunk ids the questions may cite (grounding)
	 */
	public List<TestQuestion> validateAndNormalize(List<TestQuestion> questions, Test test,
			Collection<Long> availableChunkIds, int startIndex, List<ValidationStrategy> strategies) {
		if (test == null || questions == null || questions.isEmpty()) {
			log.warn("Test {} - No questions to validate (null/empty).", test != null ? test.getId() : "null");
			return List.of();
		}

		EnumSet<ValidationStrategy> effective = resolveStrategies(strategies);
		Set<Long> available = availableChunkIds == null ? Set.of() : new HashSet<>(availableChunkIds);
		int limit = (test.getQuestionCount() != null && test.getQuestionCount() > 0) ? test.getQuestionCount()
				: Integer.MAX_VALUE;

		List<TestQuestion> valid = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		int nextIndex = startIndex;

		for (int i = 0; i < questions.size(); i++) {
			TestQuestion question = questions.get(i);
			if (question == null) {
				log.warn("Test {} - Rejected question at index {}: reason='question is null'", test.getId(), i);
				continue;
			}

			String reason = firstRejectionReason(question, test, available, effective, seen);
			if (reason != null) {
				log.warn("Test {} - Rejected question at index {}: reason='{}', question='{}'", test.getId(), i, reason,
						preview(question.getQuestionText()));
				continue;
			}

			question.setQuestionIndex(nextIndex++);
			valid.add(question);
			if (valid.size() >= limit) {
				break;
			}
		}

		if (effective.contains(ValidationStrategy.MULTI_CORRECT_QUOTA)
				&& AnswerCardinality.MULTIPLE_CORRECT.equals(test.getAnswerCardinality())) {
			int required = test.getMinMultipleCorrectQuestionsCount() != null
					? test.getMinMultipleCorrectQuestionsCount()
					: 0;
			if (!multiCorrectQuotaMet(valid, required)) {
				log.warn(
						"Test {} - Rejected validated batch: reason='multi-correct quota not met', requiredMultiCorrect={}, returnedCount={}",
						test.getId(), required, valid.size());
				return List.of();
			}
		}

		int rejected = questions.size() - valid.size();
		if (rejected > 0) {
			log.warn("Test {} - Rejected {} out of {} questions.", test.getId(), rejected, questions.size());
		}
		return valid;
	}

	/**
	 * Validates one reviewer-written question with {@link #manualStrategies()} and
	 * checks it does not duplicate another question of the test. Normalizes the
	 * question in place.
	 *
	 * @return every rejection reason; empty when valid
	 */
	public List<String> validateManual(TestQuestion question, Test test, Collection<TestQuestion> otherQuestions) {
		List<String> reasons = new ArrayList<>();
		if (question == null) {
			reasons.add("question is missing");
			return reasons;
		}
		EnumSet<ValidationStrategy> effective = resolveStrategies(manualStrategies());
		if (effective.contains(ValidationStrategy.NORMALIZE_FIELDS)) {
			normalizeFields(question);
		}
		addIfPresent(reasons, structureReason(question));
		addIfPresent(reasons, bundledReason(question));
		addIfPresent(reasons, countRevealingReason(question));
		addIfPresent(reasons, cardinalityReason(question, test));
		if (isDuplicateOfAny(question, otherQuestions)) {
			reasons.add("duplicates another question of the test");
		}
		return reasons;
	}

	/**
	 * @return true when the question matches any of the others (same normalized
	 *         text and same option set, order-independent)
	 */
	public boolean isDuplicateOfAny(TestQuestion question, Collection<TestQuestion> others) {
		if (question == null || others == null || others.isEmpty()) {
			return false;
		}
		String key = dedupeKeyOrderIndependent(question);
		String textKey = normKey(question.getQuestionText());
		for (TestQuestion other : others) {
			if (other == null || other == question) {
				continue;
			}
			if (key.equals(dedupeKeyOrderIndependent(other)) || textKey.equals(normKey(other.getQuestionText()))) {
				return true;
			}
		}
		return false;
	}

	// ---------------- per-question pipeline ----------------

	private String firstRejectionReason(TestQuestion question, Test test, Set<Long> availableChunkIds,
			EnumSet<ValidationStrategy> s, Set<String> seen) {
		if (s.contains(ValidationStrategy.NORMALIZE_FIELDS)) {
			normalizeFields(question);
		}
		if (s.contains(ValidationStrategy.STRUCTURE)) {
			String r = structureReason(question);
			if (r != null) {
				return r;
			}
		}
		if (s.contains(ValidationStrategy.GROUNDING)) {
			String r = groundingReason(question, availableChunkIds);
			if (r != null) {
				return r;
			}
		}
		if (s.contains(ValidationStrategy.BUNDLED_OPTIONS)) {
			String r = bundledReason(question);
			if (r != null) {
				return r;
			}
		}
		if (s.contains(ValidationStrategy.COUNT_REVEALING)) {
			String r = countRevealingReason(question);
			if (r != null) {
				return r;
			}
		}
		if (s.contains(ValidationStrategy.DEDUPE) && !seen.add(dedupeKeyOrderIndependent(question))) {
			return "duplicate question in batch";
		}
		if (s.contains(ValidationStrategy.CORRECT_OPTIONS_CARDINALITY)) {
			String r = cardinalityReason(question, test);
			if (r != null) {
				return r;
			}
		}
		if (s.contains(ValidationStrategy.SHUFFLE_AND_REMAP)) {
			shuffleOptions(question);
		}
		if (s.contains(ValidationStrategy.POST_SHUFFLE_SANITY)) {
			String r = postShuffleReason(question);
			if (r != null) {
				return "post-shuffle " + r;
			}
		}
		return null;
	}

	private EnumSet<ValidationStrategy> resolveStrategies(List<ValidationStrategy> strategies) {
		EnumSet<ValidationStrategy> effective = EnumSet.noneOf(ValidationStrategy.class);
		effective.addAll(strategies == null || strategies.isEmpty() ? postLlmStrategies() : strategies);
		return effective;
	}

	// ---------------- normalization ----------------

	private void normalizeFields(TestQuestion question) {
		question.setQuestionText(normalizeText(question.getQuestionText()));
		for (TestQuestionOption option : question.getOptions().values()) {
			if (option != null) {
				option.setText(normalizeText(option.getText()));
			}
		}
		question.setExplanation(normalizeText(question.getExplanation()));
	}

	// ---------------- rules (return null when OK, otherwise the reason) --------

	private String structureReason(TestQuestion q) {
		if (isBlank(q.getQuestionText())) {
			return "blank question text";
		}
		int qLen = q.getQuestionText().length();
		if (qLen < MIN_QUESTION_LEN || qLen > MAX_QUESTION_LEN) {
			return "question length out of bounds (len=" + qLen + ", allowed " + MIN_QUESTION_LEN + "-"
					+ MAX_QUESTION_LEN + ")";
		}
		if (isBlank(optionText(q, OptionLetter.A)) || isBlank(optionText(q, OptionLetter.B))) {
			return "missing mandatory options A/B";
		}
		if (requireAllFourOptions
				&& (isBlank(optionText(q, OptionLetter.C)) || isBlank(optionText(q, OptionLetter.D)))) {
			return "missing required options C/D";
		}
		for (OptionLetter letter : OptionLetter.values()) {
			if (tooLong(optionText(q, letter), MAX_OPTION_LEN)) {
				return "option " + letter + " too long (max=" + MAX_OPTION_LEN + ")";
			}
		}
		if (q.getCorrectOptions().isEmpty()) {
			return "no correct options provided";
		}
		for (OptionLetter correct : q.getCorrectOptions()) {
			if (isBlank(optionText(q, correct))) {
				return "correct option " + correct + " has blank text";
			}
		}
		if (requireExplanation && isBlank(q.getExplanation())) {
			return "missing explanation";
		}
		if (!isBlank(q.getExplanation()) && q.getExplanation().length() > MAX_EXPLANATION_LEN) {
			return "explanation too long (len=" + q.getExplanation().length() + ", max=" + MAX_EXPLANATION_LEN + ")";
		}
		if (hasDuplicateOptions(q)) {
			return "duplicate options detected";
		}
		if (hasAllOrNoneOption(q)) {
			return "contains All/None of the above option";
		}
		return null;
	}

	private String postShuffleReason(TestQuestion q) {
		if (requireAllFourOptions) {
			for (OptionLetter letter : OptionLetter.values()) {
				if (isBlank(optionText(q, letter))) {
					return "missing options";
				}
			}
		}
		if (q.getCorrectOptions().isEmpty()) {
			return "missing correct options";
		}
		for (OptionLetter correct : q.getCorrectOptions()) {
			if (isBlank(optionText(q, correct))) {
				return "correct option points to blank text";
			}
		}
		if (hasDuplicateOptions(q)) {
			return "duplicate options detected";
		}
		String bundled = bundledReason(q);
		if (bundled != null) {
			return bundled;
		}
		return countRevealingReason(q);
	}

	private String groundingReason(TestQuestion q, Set<Long> availableChunkIds) {
		if (q.getSourceChunkIds() == null) {
			return requireGrounding ? "missing source chunk ids while grounding required" : null;
		}
		List<Long> cleaned = q.getSourceChunkIds().stream().filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());
		if (!availableChunkIds.isEmpty()) {
			cleaned = cleaned.stream().filter(availableChunkIds::contains).collect(Collectors.toList());
		}
		q.setSourceChunkIds(cleaned);
		if (requireGrounding && cleaned.isEmpty()) {
			return "no valid source chunk ids after filtering";
		}
		return null;
	}

	private String bundledReason(TestQuestion q) {
		for (OptionLetter letter : OptionLetter.values()) {
			if (isBundled(optionText(q, letter))) {
				return "bundled/mega option detected in " + letter;
			}
		}
		return null;
	}

	private String countRevealingReason(TestQuestion q) {
		if (q.getCorrectOptions().size() >= 2 && q.getQuestionText() != null
				&& COUNT_REVEALING.matcher(q.getQuestionText()).find()) {
			return "count-revealing wording in multi-correct question";
		}
		return null;
	}

	private String cardinalityReason(TestQuestion q, Test test) {
		int correctCount = q.getCorrectOptions().size();
		if (AnswerCardinality.SINGLE_CORRECT.equals(test.getAnswerCardinality()) && correctCount != 1) {
			return "correct options cardinality not satisfied - question has " + correctCount
					+ " correct options instead of 1";
		}
		if (correctCount == 0) {
			return "correct options cardinality not satisfied - question has no correct option";
		}
		return null;
	}

	private boolean multiCorrectQuotaMet(List<TestQuestion> questions, int required) {
		if (required <= 0) {
			return true;
		}
		long multi = questions.stream().filter(q -> q.getCorrectOptions().size() >= 2).count();
		return multi >= required;
	}

	// ---------------- helpers ----------------

	private String dedupeKeyOrderIndependent(TestQuestion q) {
		List<String> opts = new ArrayList<>();
		for (OptionLetter letter : OptionLetter.values()) {
			String text = optionText(q, letter);
			if (!isBlank(text)) {
				opts.add(normKey(text));
			}
		}
		Collections.sort(opts);
		return normKey(q.getQuestionText()) + "||" + String.join("|", opts);
	}

	private boolean hasDuplicateOptions(TestQuestion q) {
		List<String> normalized = new ArrayList<>();
		for (OptionLetter letter : OptionLetter.values()) {
			String text = optionText(q, letter);
			if (!isBlank(text)) {
				normalized.add(normKey(text));
			}
		}
		return normalized.size() != new HashSet<>(normalized).size();
	}

	private boolean isBundled(String option) {
		if (isBlank(option)) {
			return false;
		}
		String t = option.trim();
		int separators = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == ',' || c == ';') {
				separators++;
			}
		}
		int words = t.split("\\s+").length;
		if (separators >= BUNDLE_COMMA_THRESHOLD && words >= BUNDLE_WORD_THRESHOLD) {
			String lower = t.toLowerCase(Locale.ROOT);
			return lower.contains(" and ") || lower.contains(" or ") || lower.contains(" as well as ");
		}
		return false;
	}

	private boolean hasAllOrNoneOption(TestQuestion q) {
		for (OptionLetter letter : OptionLetter.values()) {
			String text = optionText(q, letter);
			if (!isBlank(text)) {
				String t = normKey(text);
				if (t.equals("all of the above") || t.equals("none of the above") || t.equals("all of these")
						|| t.equals("none of these")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Shuffles whole option objects and reassigns letters, so the answer key and
	 * any flags move with their text. Skipped when an option is missing.
	 */
	private void shuffleOptions(TestQuestion question) {
		OptionLetter[] letters = OptionLetter.values();
		List<TestQuestionOption> options = new ArrayList<>();
		for (OptionLetter letter : letters) {
			TestQuestionOption opt = question.getOption(letter);
			if (opt == null || isBlank(opt.getText())) {
				return;
			}
			options.add(opt);
		}
		Collections.shuffle(options, ThreadLocalRandom.current());
		EnumMap<OptionLetter, TestQuestionOption> shuffled = new EnumMap<>(OptionLetter.class);
		for (int i = 0; i < letters.length; i++) {
			TestQuestionOption option = options.get(i);
			option.setLetter(letters[i]);
			shuffled.put(letters[i], option);
		}
		question.setOptions(shuffled);
	}

	private String optionText(TestQuestion q, OptionLetter letter) {
		TestQuestionOption option = q.getOption(letter);
		return option != null ? option.getText() : null;
	}

	private String normalizeText(String s) {
		if (s == null) {
			return null;
		}
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

	private static void addIfPresent(List<String> reasons, String reason) {
		if (reason != null) {
			reasons.add(reason);
		}
	}

	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private boolean tooLong(String s, int max) {
		return s != null && s.length() > max;
	}

	private String preview(String s) {
		if (s == null) {
			return "null";
		}
		String t = s.replaceAll("\\s+", " ").trim();
		return t.length() > 140 ? t.substring(0, 140) + "..." : t;
	}
}
