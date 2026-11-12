package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.GenerationRunDto;
import com.knowgauge.contract.dto.ManualQuestionInput;
import com.knowgauge.contract.dto.PublishTestInput;
import com.knowgauge.contract.dto.ReviewActionInput;
import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.contract.dto.TestEventDto;
import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.contract.dto.TestQuestionVersionDto;
import com.knowgauge.core.service.review.TestReviewService;
import com.knowgauge.restapi.mapper.TestMapper;
import com.knowgauge.restapi.mapper.TestQuestionMapper;
import com.knowgauge.restapi.mapper.TestReviewMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Review of generated questions and the test history trail (ADR001).
 */
@RestController
@RequestMapping("/api/tests/{testId}")
@Tag(name = "Test Review", description = "Review flagged questions and read the history of a test")
public class TestReviewController {

	private final TestReviewService reviewService;
	private final TestQuestionMapper questionMapper;
	private final TestReviewMapper reviewMapper;
	private final TestMapper testMapper;

	public TestReviewController(TestReviewService reviewService, TestQuestionMapper questionMapper,
			TestReviewMapper reviewMapper, TestMapper testMapper) {
		this.reviewService = reviewService;
		this.questionMapper = questionMapper;
		this.reviewMapper = reviewMapper;
		this.testMapper = testMapper;
	}

	// ---------------- actions ----------------

	@PostMapping("/questions/{questionId}/accept-fix")
	@Operation(summary = "Accept the suggested fix", description = "Creates a new version in which every flagged option's text is replaced by the verifier's suggestion. The answer key is unchanged.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Fix applied; returns the question with its new current version"),
			@ApiResponse(responseCode = "404", description = "Test or question not found"),
			@ApiResponse(responseCode = "409", description = "Question changed since expectedCurrentVersionId, test not reviewable, or no fix to apply") })
	public ResponseEntity<TestQuestionDto> acceptFix(@PathVariable Long testId, @PathVariable Long questionId,
			@RequestBody @Valid ReviewActionInput input) {
		return ResponseEntity.ok(questionMapper.toDto(
				reviewService.acceptFix(testId, questionId, input.expectedCurrentVersionId(), input.comment())));
	}

	@PostMapping("/questions/{questionId}/regenerate")
	@Operation(summary = "Regenerate the question", description = "Replaces the question with a newly generated one from the test's original context. The new version is verified again and may itself need review.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Question regenerated; returns the question with its new current version"),
			@ApiResponse(responseCode = "404", description = "Test or question not found"),
			@ApiResponse(responseCode = "409", description = "Question changed since expectedCurrentVersionId, or test not reviewable"),
			@ApiResponse(responseCode = "502", description = "The LLM could not produce a valid question; nothing was changed") })
	public ResponseEntity<TestQuestionDto> regenerate(@PathVariable Long testId, @PathVariable Long questionId,
			@RequestBody @Valid ReviewActionInput input) {
		return ResponseEntity.ok(questionMapper.toDto(
				reviewService.regenerate(testId, questionId, input.expectedCurrentVersionId(), input.comment())));
	}

	@PostMapping("/questions/{questionId}/manual-replace")
	@Operation(summary = "Replace the question manually", description = "Replaces the question with one written by the reviewer (question, options A-D with answer key, optional explanation). Validated with the non-LLM rules; not LLM-verified.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Question replaced; returns the question with its new current version"),
			@ApiResponse(responseCode = "400", description = "Question failed validation; message lists every reason"),
			@ApiResponse(responseCode = "404", description = "Test or question not found"),
			@ApiResponse(responseCode = "409", description = "Question changed since expectedCurrentVersionId, or test not reviewable") })
	public ResponseEntity<TestQuestionDto> manualReplace(@PathVariable Long testId, @PathVariable Long questionId,
			@RequestBody @Valid ManualQuestionInput input) {
		return ResponseEntity
				.ok(questionMapper.toDto(reviewService.manualReplace(testId, questionId, reviewMapper.toDraft(input))));
	}

	@PostMapping("/publish")
	@Operation(summary = "Publish the test", description = "Freezes the test: no review action can change its questions any more, and attempts can be started. Allowed only for GENERATED or REVIEWED tests with no question needing review.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Test published; returns the test with status PUBLISHED"),
			@ApiResponse(responseCode = "404", description = "Test not found"),
			@ApiResponse(responseCode = "409", description = "Test already published, not in GENERATED or REVIEWED, or has questions needing review") })
	public ResponseEntity<TestDto> publish(@PathVariable Long testId,
			@RequestBody(required = false) @Valid PublishTestInput input) {
		return ResponseEntity
				.ok(testMapper.toDto(reviewService.publish(testId, input != null ? input.comment() : null)));
	}

	// ---------------- history ----------------

	@GetMapping("/questions/{questionId}/versions")
	@Operation(summary = "List question versions", description = "All versions of the question, oldest first (version 1 = as generated).")
	public ResponseEntity<List<TestQuestionVersionDto>> getVersions(@PathVariable Long testId,
			@PathVariable Long questionId) {
		return ResponseEntity
				.ok(reviewService.getVersions(testId, questionId).stream().map(questionMapper::toDto).toList());
	}

	@GetMapping("/events")
	@Operation(summary = "Test history trail", description = "Every generation, flag, review action and status change of the test, oldest first.")
	public ResponseEntity<List<TestEventDto>> getEvents(@PathVariable Long testId,
			@Parameter(description = "Only events of this question") @RequestParam(required = false) Long questionId) {
		return ResponseEntity
				.ok(reviewService.getEvents(testId, questionId).stream().map(reviewMapper::toDto).toList());
	}

	@GetMapping("/generation-runs")
	@Operation(summary = "LLM calls of the test", description = "Every generation, regeneration and verification call with model, tokens and raw response, oldest first.")
	public ResponseEntity<List<GenerationRunDto>> getGenerationRuns(@PathVariable Long testId) {
		return ResponseEntity
				.ok(reviewService.getGenerationRuns(testId).stream().map(reviewMapper::toDto).toList());
	}
}
