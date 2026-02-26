package com.knowgauge.infra.testgeneration.langchain4j.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerVerificationDto {

	private String option; // "A" | "B" | "C" | "D"
	private String originalAnswer;
	private Boolean verdict;
	private String replacementAnswer;
	private String explanation;

}