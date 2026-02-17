package com.knowgauge.infra.testgeneration.openai.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerOption;
import com.knowgauge.infra.testgeneration.openai.dto.TestQuestionDto;
import com.knowgauge.infra.testgeneration.openai.dto.TestQuestionResponseDto;

import dev.langchain4j.model.chat.response.ChatResponse;

@Component
public class ChatResponseMapper {

	private final ObjectMapper objectMapper;

	public ChatResponseMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<TestQuestion> map(ChatResponse response, Test test) {

		String raw = extractRawText(response);
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}

		String json = extractJsonObject(raw);

		try {

			TestQuestionResponseDto dto = objectMapper.readValue(json, TestQuestionResponseDto.class);

			if (dto == null || dto.getQuestions() == null || dto.getQuestions().isEmpty()) {
				return Collections.emptyList();
			}

			return toDomain(dto.getQuestions(), test);

		} catch (Exception e) {

			throw new IllegalStateException("Failed to parse LLM response. Raw response:\n" + raw, e);
		}
	}

	private List<TestQuestion> toDomain(List<TestQuestionDto> dtos, Test test) {

		List<TestQuestion> result = new ArrayList<>();

		int index = 0;

		for (TestQuestionDto dto : dtos) {

			TestQuestion q = new TestQuestion();

			q.setTenantId(test.getTenantId());
			q.setTestId(test.getId());

			q.setQuestionIndex(index++);

			q.setQuestionText(dto.getQuestion());

			if (dto.getOptions() != null) {
				q.setOptionA(dto.getOptions().get("A"));
				q.setOptionB(dto.getOptions().get("B"));
				q.setOptionC(dto.getOptions().get("C"));
				q.setOptionD(dto.getOptions().get("D"));
			}

			q.setCorrectOption(parseAnswerOption(dto.getCorrect()));
			q.setExplanation(dto.getExplanation());

			q.setSourceChunkIdsJson(parseChunkIds(dto.getSources()));

			result.add(q);
		}

		return result;
	}

	private AnswerOption parseAnswerOption(String correct) {
		if (correct == null || correct.isBlank()) {
			return null;
		}
		try {
			return AnswerOption.valueOf(correct.trim().toUpperCase());
		} catch (Exception e) {
			return null; // or throw if you prefer strictness
		}
	}

	private List<Long> parseChunkIds(List<String> sources) {
		if (sources == null || sources.isEmpty()) {
			return Collections.emptyList();
		}
		return sources.stream().map(this::extractNumericId).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private Long extractNumericId(String source) {
		if (source == null)
			return null;

		// supports: "chunk12", "chunk-12", "12"
		String digits = source.replaceAll("[^0-9]", "");
		if (digits.isBlank())
			return null;

		try {
			return Long.valueOf(digits);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String extractRawText(ChatResponse response) {
		if (response == null || response.aiMessage() == null) {
			return null;
		}
		return response.aiMessage().text();
	}

	private String extractJsonObject(String raw) {
		int start = raw.indexOf('{');
		int end = raw.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return raw.substring(start, end + 1);
		}
		return raw.trim();
	}
}
