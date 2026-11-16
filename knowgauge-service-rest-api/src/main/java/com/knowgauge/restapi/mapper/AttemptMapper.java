package com.knowgauge.restapi.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;

import com.knowgauge.contract.dto.AttemptDetailDto;
import com.knowgauge.contract.dto.AttemptDto;
import com.knowgauge.contract.dto.AttemptQuestionDto;
import com.knowgauge.contract.dto.SubmitAttemptInput;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AttemptStatus;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.service.attempt.AttemptService.AttemptView;
import com.knowgauge.restapi.util.EnumParser;

@Mapper(componentModel = "spring")
public interface AttemptMapper {

	AttemptDto toDto(Attempt attempt);

	/**
	 * The answer key, the chosen options and the explanation are only included once
	 * the attempt is scored, so they cannot be read while the test is being taken.
	 */
	default AttemptDetailDto toDetailDto(AttemptView view) {
		boolean scored = view.attempt().getStatus() == AttemptStatus.SCORED;
		Map<Long, AttemptAnswer> answers = view.answers().stream()
				.collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity()));
		List<AttemptQuestionDto> questions = view.questions().stream()
				.map(q -> toQuestionDto(q, scored ? answers.get(q.getId()) : null, scored)).toList();
		return new AttemptDetailDto(toDto(view.attempt()), view.test().getName(),
				view.test().getAnswerCardinality() == null ? null : view.test().getAnswerCardinality().name(),
				questions);
	}

	private AttemptQuestionDto toQuestionDto(TestQuestion question, AttemptAnswer answer, boolean scored) {
		Map<String, String> options = new LinkedHashMap<>();
		question.getOptions().forEach((letter, option) -> options.put(letter.name(), option.getText()));
		return new AttemptQuestionDto(question.getId(), question.getCurrentVersionId(), question.getQuestionIndex(),
				question.getQuestionText(), options,
				scored ? letters(answer == null ? List.of() : answer.getChosenOptions()) : null,
				scored ? letters(question.getCorrectOptions()) : null,
				scored ? answer != null && Boolean.TRUE.equals(answer.getCorrect()) : null,
				scored ? question.getExplanation() : null);
	}

	private static List<String> letters(List<OptionLetter> letters) {
		return letters == null ? List.of() : letters.stream().sorted().map(OptionLetter::name).toList();
	}

	/** Chosen options per question id; letters are parsed case-insensitively. */
	default Map<Long, List<OptionLetter>> toChosenOptions(SubmitAttemptInput input) {
		Map<Long, List<OptionLetter>> chosen = new LinkedHashMap<>();
		for (SubmitAttemptInput.Answer answer : input.answers()) {
			List<OptionLetter> letters = answer.chosenOptions().stream()
					.map(letter -> EnumParser.parse(OptionLetter.class, letter)).toList();
			if (chosen.put(answer.questionId(), letters) != null) {
				throw new IllegalArgumentException("Question " + answer.questionId() + " is answered twice.");
			}
		}
		return chosen;
	}
}
