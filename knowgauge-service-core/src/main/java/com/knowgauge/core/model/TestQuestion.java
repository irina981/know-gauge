package com.knowgauge.core.model;

import java.util.ArrayList;
import java.util.List;

import com.knowgauge.core.model.enums.AnswerOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestion extends AuditableObject {
	private Long tenantId;

	private Long testId;

	private Integer questionIndex;

	private String questionText;

	private String optionA;

	private String optionB;

	private String optionC;

	private String optionD;

	@Builder.Default
	private List<AnswerOption> correctOptions = new ArrayList<AnswerOption>();

	private String explanation;

	private List<Long> sourceChunkIdsJson;

}
