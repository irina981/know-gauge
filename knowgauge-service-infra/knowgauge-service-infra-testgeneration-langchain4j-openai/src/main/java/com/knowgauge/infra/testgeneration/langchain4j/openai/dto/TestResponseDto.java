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
public class TestQuestionResponseDto {

    private List<TestQuestionDto> questions;
}
