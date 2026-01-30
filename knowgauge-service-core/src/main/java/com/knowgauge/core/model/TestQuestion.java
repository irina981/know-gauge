package com.knowgauge.core.model;

import java.time.Instant;
import java.util.List;

import com.knowgauge.core.model.enums.AnswerOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestQuestion {

	private Long id;

	private Long testId;

	private Integer questionIndex;

	private String questionText;

	private String optionA;

	private String optionB;

	private String optionC;

	private String optionD;

	private AnswerOption correctOption;

	private String explanation;

	private  List<Long> sourceChunkIdsJson;

	private Instant createdAt;

}
