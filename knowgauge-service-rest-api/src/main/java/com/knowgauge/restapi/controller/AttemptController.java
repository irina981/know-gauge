package com.knowgauge.restapi.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.knowgauge.contract.dto.AttemptDetailDto;
import com.knowgauge.contract.dto.AttemptDto;
import com.knowgauge.contract.dto.SubmitAttemptInput;
import com.knowgauge.core.service.attempt.AttemptService;
import com.knowgauge.core.service.attempt.AttemptService.AttemptView;
import com.knowgauge.restapi.mapper.AttemptMapper;
import com.knowgauge.restapi.security.Roles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.annotation.security.RolesAllowed;

/**
 * Taking published tests: start, submit (scored at once) and list attempts.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Attempts", description = "Take published tests and read the results")
public class AttemptController {

	private final AttemptService attemptService;
	private final AttemptMapper attemptMapper;

	public AttemptController(AttemptService attemptService, AttemptMapper attemptMapper) {
		this.attemptService = attemptService;
		this.attemptMapper = attemptMapper;
	}

	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	@PostMapping("/tests/{testId}/attempts")
	@Operation(summary = "Start an attempt", description = "Starts an attempt on a published test for the current user. Returns the questions without the answer key.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Attempt started"),
			@ApiResponse(responseCode = "404", description = "Test not found"),
			@ApiResponse(responseCode = "409", description = "Test is not published or has no questions") })
	public ResponseEntity<AttemptDetailDto> start(@PathVariable Long testId) {
		AttemptView view = attemptService.start(testId);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/attempts/{id}")
				.buildAndExpand(view.attempt().getId()).toUri();
		return ResponseEntity.created(location).body(attemptMapper.toDetailDto(view));
	}

	@GetMapping("/attempts/{attemptId}")
	@Operation(summary = "Get an attempt", description = "Questions without the answer key while the attempt is running; with the chosen and correct options and explanations once it is scored.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt returned"),
			@ApiResponse(responseCode = "404", description = "Attempt not found") })
	public ResponseEntity<AttemptDetailDto> get(@PathVariable Long attemptId) {
		return ResponseEntity.ok(attemptMapper.toDetailDto(attemptService.get(attemptId)));
	}

	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	@PostMapping("/attempts/{attemptId}/submit")
	@Operation(summary = "Submit and score an attempt", description = "Saves the answers and scores the attempt. A question is correct only if the chosen options exactly match the answer key; questions left out count as wrong.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt scored; returns the full review"),
			@ApiResponse(responseCode = "400", description = "Invalid option letter, question not in the test, or several options on a single-answer test"),
			@ApiResponse(responseCode = "404", description = "Attempt not found"),
			@ApiResponse(responseCode = "409", description = "Attempt already submitted") })
	public ResponseEntity<AttemptDetailDto> submit(@PathVariable Long attemptId,
			@RequestBody @Valid SubmitAttemptInput input) {
		return ResponseEntity.ok(
				attemptMapper.toDetailDto(attemptService.submit(attemptId, attemptMapper.toChosenOptions(input))));
	}

	@GetMapping("/attempts")
	@Operation(summary = "List attempts", description = "Attempts of the tenant, newest first, optionally filtered by test, user and participant email.")
	public ResponseEntity<List<AttemptDto>> list(
			@Parameter(description = "Only attempts at this test") @RequestParam(required = false) Long testId,
			@Parameter(description = "Only attempts by this user") @RequestParam(required = false) Long userId,
			@Parameter(description = "Only attempts by this participant email (case-insensitive)") @RequestParam(required = false) String participantEmail) {
		return ResponseEntity.ok(attemptService.list(testId, userId, participantEmail).stream().map(attemptMapper::toDto).toList());
	}
}
