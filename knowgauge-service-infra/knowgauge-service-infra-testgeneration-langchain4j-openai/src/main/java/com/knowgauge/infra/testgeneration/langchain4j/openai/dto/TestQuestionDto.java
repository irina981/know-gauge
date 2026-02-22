package com.knowgauge.infra.testgeneration.langchain4j.openai.dto;

import java.util.List;
import java.util.Map;

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

    private String question;

    private Map<String, String> options; // keys: A,B,C,D

    private String correct; // "A" | "B" | "C" | "D"

    private String explanation;

    private List<String> sources; // e.g. ["chunk12","chunk45"]
}

