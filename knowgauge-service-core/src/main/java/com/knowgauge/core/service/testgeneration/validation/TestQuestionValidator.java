package com.knowgauge.core.service.testgeneration.validation;

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
 * Validates and normalizes LLM-generated TestQuestion objects (POST-GENERATION).
 *
 * Responsibilities:
 * - Ensure structural validity of generated questions (question text, options, correct answer)
 * - Normalize text fields
 * - Optional explanation enforcement
 * - Optional grounding enforcement (sourceChunkIdsJson)
 * - Remove duplicates within a generation batch
 * - Normalize questionIndex to sequential values
 * - Enforce test.questionCount limit
 *
 * Thread-safe and stateless.
 */
@Component
public class TestQuestionValidator {

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

    public List<TestQuestion> validateAndNormalize(List<TestQuestion> questions, Test test, List<ChunkEmbedding> embeddings) {

        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        int limit = (test != null && test.getQuestionCount() != null && test.getQuestionCount() > 0)
                ? test.getQuestionCount()
                : Integer.MAX_VALUE;

        Set<Long> availableChunkIds = buildAvailableChunkIds(embeddings);

        List<TestQuestion> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int nextIndex = 0;

        for (TestQuestion q : questions) {

            if (q == null) continue;

            normalizeFields(q);

            if (!isValidStructure(q)) continue;

            if (!validateGrounding(q, availableChunkIds)) continue;

            String key = dedupeKey(q);
            if (!seen.add(key)) continue;

            q.setQuestionIndex(nextIndex++);
            out.add(q);

            if (out.size() >= limit) break;
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

        if (isBlank(q.getQuestionText())) return false;

        if (q.getQuestionText().length() < 12) return false;

        if (isBlank(q.getOptionA()) || isBlank(q.getOptionB())) return false;

        if (requireAllFourOptions) {
            if (isBlank(q.getOptionC()) || isBlank(q.getOptionD())) return false;
        }

        if (q.getCorrectOption() == null) return false;

        if (isBlank(optionText(q, q.getCorrectOption()))) return false;

        if (requireExplanation && isBlank(q.getExplanation())) return false;

        if (hasDuplicateOptions(q)) return false;

        return true;
    }

    // ---------------- grounding validation ----------------

    private boolean validateGrounding(TestQuestion q, Set<Long> availableChunkIds) {

        if (q.getSourceChunkIdsJson() == null) {
            return !requireGrounding;
        }

        List<Long> cleaned = q.getSourceChunkIdsJson().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!availableChunkIds.isEmpty()) {
            cleaned = cleaned.stream().filter(availableChunkIds::contains).toList();
        }

        q.setSourceChunkIdsJson(cleaned);

        return !(requireGrounding && cleaned.isEmpty());
    }

    // ---------------- duplicate prevention ----------------

    private String dedupeKey(TestQuestion q) {

        return normKey(q.getQuestionText())
                + "||" + normKey(q.getOptionA())
                + "|" + normKey(q.getOptionB())
                + "|" + normKey(q.getOptionC())
                + "|" + normKey(q.getOptionD());
    }

    // ---------------- helpers ----------------

    private boolean hasDuplicateOptions(TestQuestion q) {

        List<String> opts = List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());

        List<String> normalized = opts.stream()
                .filter(s -> !isBlank(s))
                .map(this::normKey)
                .toList();

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

        if (s == null) return null;

        String t = Normalizer.normalize(s, Normalizer.Form.NFKC);

        t = t.trim().replaceAll("\\s+", " ");

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

    private Set<Long> buildAvailableChunkIds(List<ChunkEmbedding> embeddings) {

        if (embeddings == null || embeddings.isEmpty()) {
            return Set.of();
        }

        // NOTE: You can simplify this to embeddings.stream().map(ChunkEmbedding::getChunkId).collect(...)
        // if ChunkEmbedding has getChunkId() in the interface.
        Set<Long> ids = new HashSet<>();

        for (ChunkEmbedding e : embeddings) {
            try {
                var m = e.getClass().getMethod("getChunkId");
                Object v = m.invoke(e);
                if (v instanceof Long l) ids.add(l);
            } catch (Exception ignored) {
            }
        }

        return ids;
    }
}
