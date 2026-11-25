package com.knowgauge.contract.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.knowgauge.contract.dto.AttemptDetailDto;
import com.knowgauge.contract.dto.AttemptDto;
import com.knowgauge.contract.dto.SubmitAttemptInput;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Taking published tests: start, submit (scored at once) and list attempts.
 */
@RequestMapping("/api")
@Tag(name = "Attempts", description = "Take published tests and read the results")
public interface AttemptApi {

	@PostMapping("/tests/{testId}/attempts")
	@Operation(summary = "Start an attempt", description = "Starts an attempt on a published test for the current user. Returns the questions without the answer key.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Attempt started"),
			@ApiResponse(responseCode = "404", description = "Test not found"),
			@ApiResponse(responseCode = "409", description = "Test is not published or has no questions") })
	ResponseEntity<AttemptDetailDto> start(@PathVariable Long testId);

	@GetMapping("/attempts/{attemptId}")
	@Operation(summary = "Get an attempt", description = "Questions without the answer key while the attempt is running; with the chosen and correct options and explanations once it is scored.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt returned"),
			@ApiResponse(responseCode = "404", description = "Attempt not found") })
	ResponseEntity<AttemptDetailDto> get(@PathVariable Long attemptId);

	@PostMapping("/attempts/{attemptId}/submit")
	@Operation(summary = "Submit and score an attempt", description = "Saves the answers and scores the attempt. A question is correct only if the chosen options exactly match the answer key; questions left out count as wrong.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt scored; returns the full review"),
			@ApiResponse(responseCode = "400", description = "Invalid option letter, question not in the test, or several options on a single-answer test"),
			@ApiResponse(responseCode = "404", description = "Attempt not found"),
			@ApiResponse(responseCode = "409", description = "Attempt already submitted") })
	ResponseEntity<AttemptDetailDto> submit(@PathVariable Long attemptId, @RequestBody @Valid SubmitAttemptInput input);

	@GetMapping("/attempts")
	@Operation(summary = "List attempts", description = "Attempts of the tenant, newest first, optionally filtered by test, user and participant email.")
	ResponseEntity<List<AttemptDto>> list(
			@Parameter(description = "Only attempts at this test") @RequestParam(required = false) Long testId,
			@Parameter(description = "Only attempts by this user") @RequestParam(required = false) Long userId,
			@Parameter(description = "Only attempts by this participant email (case-insensitive)") @RequestParam(required = false) String participantEmail);
}
