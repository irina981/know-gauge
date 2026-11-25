package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.TestReviewApi;
import com.knowgauge.contract.dto.DismissFlagsInput;
import com.knowgauge.contract.dto.GenerationRunDto;
import com.knowgauge.contract.dto.ManualQuestionInput;
import com.knowgauge.contract.dto.PublishTestInput;
import com.knowgauge.contract.dto.ReviewActionInput;
import com.knowgauge.contract.dto.TestDetailsInput;
import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.contract.dto.TestEventDto;
import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.contract.dto.TestQuestionVersionDto;
import com.knowgauge.core.service.review.TestReviewService;
import com.knowgauge.restapi.mapper.TestMapper;
import com.knowgauge.restapi.mapper.TestQuestionMapper;
import com.knowgauge.restapi.mapper.TestReviewMapper;
import com.knowgauge.restapi.security.Roles;

import jakarta.annotation.security.RolesAllowed;

/**
 * Review of generated questions and the test history trail (ADR001).
 */
@RestController
public class TestReviewController implements TestReviewApi {

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

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestQuestionDto> acceptFix(Long testId, Long questionId, ReviewActionInput input) {
		return ResponseEntity.ok(questionMapper.toDto(
				reviewService.acceptFix(testId, questionId, input.expectedCurrentVersionId(), input.comment())));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestQuestionDto> dismissFlags(Long testId, Long questionId, DismissFlagsInput input) {
		return ResponseEntity.ok(questionMapper.toDto(
				reviewService.dismissFlags(testId, questionId, input.expectedCurrentVersionId(), input.comment())));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestQuestionDto> regenerate(Long testId, Long questionId, ReviewActionInput input) {
		return ResponseEntity.ok(questionMapper.toDto(
				reviewService.regenerate(testId, questionId, input.expectedCurrentVersionId(), input.comment())));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestQuestionDto> manualReplace(Long testId, Long questionId, ManualQuestionInput input) {
		return ResponseEntity
				.ok(questionMapper.toDto(reviewService.manualReplace(testId, questionId, reviewMapper.toDraft(input))));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestDto> updateDetails(Long testId, TestDetailsInput input) {
		return ResponseEntity
				.ok(testMapper.toDto(reviewService.updateDetails(testId, input.name(), input.description())));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestDto> publish(Long testId, PublishTestInput input) {
		return ResponseEntity
				.ok(testMapper.toDto(reviewService.publish(testId, input != null ? input.comment() : null)));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestDto> unpublish(Long testId, PublishTestInput input) {
		return ResponseEntity
				.ok(testMapper.toDto(reviewService.unpublish(testId, input != null ? input.comment() : null)));
	}

	@Override
	public ResponseEntity<Boolean> hasAttempts(Long testId) {
		return ResponseEntity.ok(reviewService.hasAttempts(testId));
	}

	// ---------------- history ----------------

	@Override
	public ResponseEntity<List<TestQuestionVersionDto>> getVersions(Long testId, Long questionId) {
		return ResponseEntity
				.ok(reviewService.getVersions(testId, questionId).stream().map(questionMapper::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TestEventDto>> getEvents(Long testId, Long questionId) {
		return ResponseEntity
				.ok(reviewService.getEvents(testId, questionId).stream().map(reviewMapper::toDto).toList());
	}

	@Override
	public ResponseEntity<List<GenerationRunDto>> getGenerationRuns(Long testId) {
		return ResponseEntity
				.ok(reviewService.getGenerationRuns(testId).stream().map(reviewMapper::toDto).toList());
	}
}
