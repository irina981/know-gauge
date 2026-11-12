package com.knowgauge.infra.testgeneration.langchain4j.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.knowgauge.core.model.enums.OptionLetter;

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
public class OptionVerificationDto {

	@JsonProperty("option")
	private OptionLetter letter; // "A" | "B" | "C" | "D"
	
	@JsonProperty("originalAnswer")
	private String originalText;
	
	@JsonProperty("verdict")
	private Boolean verdict;
	
	@JsonProperty("replacementAnswer")
	private String replacementText;
	
	@JsonProperty("explanation")
	private String explanation;

}