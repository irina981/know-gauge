package com.knowgauge.core.model;

import java.util.List;

import com.knowgauge.core.model.enums.AnswerOption;

import lombok.AllArgsConstructor;
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

	private Long testId;

	private Integer questionIndex;

	private String questionText;

	private String optionA;

	private String optionB;

	private String optionC;

	private String optionD;

	private AnswerOption correctOption;

	private String explanation;

	private List<Long> sourceChunkIdsJson;

}
