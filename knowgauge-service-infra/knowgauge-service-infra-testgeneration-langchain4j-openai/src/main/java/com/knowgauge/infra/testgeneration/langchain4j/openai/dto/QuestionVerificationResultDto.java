package com.knowgauge.infra.testgeneration.langchain4j.openai.dto;

import java.util.List;

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

    private Integer questionIndex;   
    private String questionText;
    private List<AnswerVerificationDto> evaluatedAnswers;


}