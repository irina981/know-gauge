package com.knowgauge.core.model;

import java.time.Instant;
import java.util.List;

import com.knowgauge.core.model.enums.GenerationRunStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationRun {

	private Long id;

	private Long testId;

	private String model;

	private String promptTemplateVersion;

	private  List<Long> retrievedChunkIdsJson;

	private String rawResponse;

	private GenerationRunStatus status;

	private String errorMessage;

	private Instant createdAt;

}
