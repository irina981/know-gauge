package com.knowgauge.infra.testgeneration.langchain4j.openai.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerOption;
import com.knowgauge.infra.testgeneration.langchain4j.openai.dto.TestQuestionDto;
import com.knowgauge.infra.testgeneration.langchain4j.openai.dto.TestQuestionResponseDto;

import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TestGenerationResponseMapper {

	private final ObjectMapper objectMapper;

	public TestGenerationResponseMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<TestQuestion> map(ChatResponse response, Test test) {

		String raw = extractRawText(response);
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}

		String json = extractJsonObject(raw);
		log.info("JSON extracted from LLM response (testId={}): {}", test.getId(), json);

		try {

			TestQuestionResponseDto dto = objectMapper.readValue(json, TestQuestionResponseDto.class);

			if (dto == null || dto.getQuestions() == null || dto.getQuestions().isEmpty()) {
				return Collections.emptyList();
			}

			return toDomain(dto.getQuestions(), test);

		} catch (JsonMappingException e) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.MAPPING_ERROR,
					"Failed to map LLM response for testId=" + test.getId() + ". Raw response:\n" + raw, e);
		} catch (JsonProcessingException e) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.PARSING_ERROR,
					"Failed to parse LLM response for testId=" + test.getId() + ". Raw response:\n" + raw, e);
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

			q.setCorrectOptions(parseAnswerOptions(dto));
			q.setExplanation(dto.getExplanation());

			q.setSourceChunkIdsJson(parseChunkIds(dto.getSources()));

			result.add(q);
		}

		return result;
	}

	private List<AnswerOption> parseAnswerOptions(TestQuestionDto dto) {
		if (dto.getCorrectOptions() != null && !dto.getCorrectOptions().isEmpty()) {
			return dto.getCorrectOptions().stream().map(this::parseAnswerOption).filter(Objects::nonNull).distinct()
					.toList();
		}

		AnswerOption fallback = parseAnswerOption(dto.getCorrect());
		return fallback == null ? Collections.emptyList() : List.of(fallback);
	}

	private AnswerOption parseAnswerOption(String option) {
		if (option == null || option.isBlank()) {
			return null;
		}
		try {
			return AnswerOption.valueOf(option.trim().toUpperCase());
		} catch (Exception e) {
			return null;
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
