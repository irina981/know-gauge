package com.knowgauge.infra.testgeneration.langchain4j.openai.dto;

import java.util.List;
import java.util.Map;

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
public class TestQuestionDto {

    @JsonProperty("question")
    private String question;

    @JsonProperty("options")
    private Map<String, String> options; // keys: A,B,C,D

    @JsonProperty("correctOptions")
    private List<String> correctOptions; // ["A", "C"]

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("sources")
    private List<String> sources; // e.g. ["chunk12","chunk45"]
}

