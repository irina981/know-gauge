package com.knowgauge.infra.testgeneration.langchain4j.openai.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.infra.testgeneration.langchain4j.openai.dto.AnswerVerificationDto;
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

	public int mapAndApplyReplacements(ChatResponse response, List<TestQuestion> questions, Test test) {

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

			return applyReplacements(questions, dto.getResults(), test);

		} catch (JsonMappingException e) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.MAPPING_ERROR,
					"Failed to map verification response for testId=" + test.getId() + ". Raw response:\n" + raw, e);
		} catch (JsonProcessingException e) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.PARSING_ERROR,
					"Failed to parse verification response for testId=" + test.getId() + ". Raw response:\n" + raw, e);
		}
	}

	private int applyReplacements(List<TestQuestion> questions, List<QuestionVerificationResultDto> results,
			Test test) {

		int replacedCount = 0;

		for (QuestionVerificationResultDto result : results) {
			Integer questionIndex = result.getQuestionIndex();

			if (questionIndex == null || questionIndex < 0 || questionIndex >= questions.size()) {
				log.warn("Test {} - Invalid questionIndex {} in verification response.", test.getId(), questionIndex);
				continue;
			}

			TestQuestion question = questions.get(questionIndex);

			if (result.getEvaluatedAnswers() != null) {
				for (AnswerVerificationDto evaluatedAnswer : result.getEvaluatedAnswers()) {
					if (evaluatedAnswer == null || evaluatedAnswer.getVerdict() == null
							|| evaluatedAnswer.getOption() == null) {
						log.warn("Test {} - Invalid evaluated answer in questionIndex = {} in verification response.",
								test.getId(), questionIndex);
						continue;
					}

					String optionKey = evaluatedAnswer.getOption();
					
					// Answer is verified as incorrect option -> skipping it
					if (evaluatedAnswer.getVerdict() == Boolean.FALSE) {
						log.warn(
								"Test {} - Incorrect option {} - {} for questionIndex = {} found in verification response, ignorring it.",
								test.getId(), evaluatedAnswer.getOption(), evaluatedAnswer.getOriginalAnswer(),
								questionIndex);
						continue;
					}

					// Answer is verified as correct option (invalid distractor) -> replace it with provided valid distractor
 					String oldText = getOptionText(question, optionKey);
					String newText = evaluatedAnswer.getReplacementAnswer();
					boolean replaced = replaceOption(question, optionKey, newText);

					if (replaced) {
						replacedCount++;
						log.info("Test {} - Replaced unsafe option {} in question {}: '{}' -> '{}' (reason: {})",
								test.getId(), optionKey, questionIndex, oldText, newText,
								evaluatedAnswer.getExplanation() != null ? evaluatedAnswer.getExplanation() : "N/A");
					}
				}
			}
		}

		return replacedCount;
	}

	private boolean replaceOption(TestQuestion question, String optionKey, String newText) {
		switch (optionKey.toUpperCase()) {
		case "A":
			question.setOptionA(newText);
			return true;
		case "B":
			question.setOptionB(newText);
			return true;
		case "C":
			question.setOptionC(newText);
			return true;
		case "D":
			question.setOptionD(newText);
			return true;
		default:
			log.warn("Unknown option key: {}", optionKey);
			return false;
		}
	}

	private String getOptionText(TestQuestion question, String optionKey) {
		return switch (optionKey.toUpperCase()) {
		case "A" -> question.getOptionA();
		case "B" -> question.getOptionB();
		case "C" -> question.getOptionC();
		case "D" -> question.getOptionD();
		default -> "?";
		};
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
