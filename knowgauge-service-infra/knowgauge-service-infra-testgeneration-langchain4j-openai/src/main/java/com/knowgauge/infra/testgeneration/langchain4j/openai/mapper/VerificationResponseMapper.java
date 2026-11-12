package com.knowgauge.infra.testgeneration.langchain4j.openai.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.infra.testgeneration.langchain4j.openai.dto.OptionVerificationDto;
import com.knowgauge.infra.testgeneration.langchain4j.openai.dto.QuestionVerificationResultDto;
import com.knowgauge.infra.testgeneration.langchain4j.openai.dto.VerificationResponseDto;

import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VerificationResponseMapper {

	private final ObjectMapper objectMapper;

	public VerificationResponseMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public int mapAndApplyVerificationResult(ChatResponse response, List<TestQuestion> questions, Test test) {

		String raw = extractRawText(response);
		if (raw == null || raw.isBlank()) {
			log.warn("Empty verification response for testId={}", test.getId());
			return 0;
		}

		String json = extractJsonObject(raw);
		log.info("JSON extracted from verification response (testId={}): {}", test.getId(), json);

		try {

			VerificationResponseDto dto = objectMapper.readValue(json, VerificationResponseDto.class);

			if (dto == null || dto.getResults() == null || dto.getResults().isEmpty()) {
				log.info("No verification results found for testId={}", test.getId());
				return 0;
			}

			return applyVerificationResult(questions, dto.getResults(), test);

		} catch (JsonMappingException e) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.MAPPING_ERROR,
					"Failed to map verification response for testId=" + test.getId() + ". Raw response:\n" + raw, e);
		} catch (JsonProcessingException e) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.PARSING_ERROR,
					"Failed to parse verification response for testId=" + test.getId() + ". Raw response:\n" + raw, e);
		}
	}

	private int applyVerificationResult(List<TestQuestion> questions, List<QuestionVerificationResultDto> results,
			Test test) {

		int flaggedCount = 0;

		for (QuestionVerificationResultDto result : results) {
			Integer questionIndex = result.getQuestionIndex();

			if (questionIndex == null || questionIndex < 0 || questionIndex >= questions.size()) {
				log.warn("Test {} - Invalid questionIndex {} in verification response.", test.getId(), questionIndex);
				continue;
			}

			TestQuestion question = questions.get(questionIndex);

			if (result.getEvaluatedOptions() != null) {
				for (OptionVerificationDto evaluatedOption : result.getEvaluatedOptions()) {
					if (evaluatedOption == null || evaluatedOption.getVerdict() == null
							|| evaluatedOption.getLetter() == null) {
						log.warn("Test {} - Invalid evaluated answer in questionIndex = {} in verification response.",
								test.getId(), questionIndex);
						continue;
					}

					OptionLetter optionLetter = evaluatedOption.getLetter();
					TestQuestionOption option = question.getOption(optionLetter);
					if (option == null) {
						log.warn("Test {} - Verification referenced missing option {} in questionIndex = {}.",
								test.getId(), optionLetter, questionIndex);
						continue;
					}

					// A distractor the verifier judges TRUE is flagged for review. The answer key
					// is never changed by verification (ADR001).
					if (Boolean.TRUE.equals(evaluatedOption.getVerdict()) && !option.isCorrect()
							&& !option.isFlagged()) {
						option.flag(evaluatedOption.getReplacementText(), evaluatedOption.getExplanation());
						log.info("Test {} - Flagged option {} - {} in question: '{}' (reason: {})", test.getId(),
								optionLetter, option.getText(), questionIndex,
								evaluatedOption.getExplanation() != null ? evaluatedOption.getExplanation() : "N/A");
						flaggedCount++;
					}

				}
			}
		}

		return flaggedCount;
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
