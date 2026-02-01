package com.knowgauge.core.model;

import java.util.List;

import com.knowgauge.core.model.enums.GenerationRunStatus;

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
public class GenerationRun extends AuditableObject {

	private Long id;

	private Long testId;

	private String model;

	private String promptTemplateVersion;

	private List<Long> retrievedChunkIdsJson;

	private String rawResponse;

	private GenerationRunStatus status;

	private String errorMessage;

}
