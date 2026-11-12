package com.knowgauge.infra.testgeneration.langchain4j.openai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class QuestionVerificationResultDto {

    @JsonProperty("questionIndex")
    private Integer questionIndex;   
    
    @JsonProperty("questionText")
    private String questionText;
    
    @JsonProperty("evaluatedAnswers")
    private List<OptionVerificationDto> evaluatedOptions;


}