package com.knowgauge.restapi.mapper;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.knowgauge.contract.dto.GenerationRunDto;
import com.knowgauge.contract.dto.ManualQuestionInput;
import com.knowgauge.contract.dto.TestEventDto;
import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.model.TestEvent;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.service.review.ManualQuestionDraft;
import com.knowgauge.core.service.review.ManualQuestionDraft.ManualOption;
import com.knowgauge.restapi.util.EnumParser;

/**
 * Maps review inputs and history outputs between API DTOs and core.
 */
@Component
public class TestReviewMapper {

	public ManualQuestionDraft toDraft(ManualQuestionInput input) {
		Map<OptionLetter, ManualOption> options = new EnumMap<>(OptionLetter.class);
		if (input.options() != null) {
			input.options().forEach((key, option) -> {
				OptionLetter letter = EnumParser.parse(OptionLetter.class, key);
				if (letter != null && option != null) {
					options.put(letter, new ManualOption(option.text(), Boolean.TRUE.equals(option.correct())));
				}
			});
		}
		return new ManualQuestionDraft(input.expectedCurrentVersionId(), input.questionText(), options,
				input.explanation(), input.comment());
	}

	public TestEventDto toDto(TestEvent e) {
		return new TestEventDto(e.getId(), e.getTestId(), e.getQuestionId(),
				e.getEventType() != null ? e.getEventType().name() : null, e.getFromVersionId(), e.getToVersionId(),
				e.getActorUserId(), e.getComment(), e.getPayload(), e.getCreatedAt());
	}

	public GenerationRunDto toDto(GenerationRun r) {
		return new GenerationRunDto(r.getId(), r.getTestId(), r.getQuestionId(),
				r.getPurpose() != null ? r.getPurpose().name() : null, r.getBatchIndex(), r.getModel(),
				r.getPromptTemplateId(), r.getRetrievedChunkIds(), r.getRawResponse(), r.getInputTokens(),
				r.getOutputTokens(), r.getFinishReason(), r.getStatus() != null ? r.getStatus().name() : null,
				r.getErrorMessage(), r.getCreatedAt());
	}
}
