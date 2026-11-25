package com.knowgauge.contract.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.knowgauge.contract.dto.PublicAttemptDto;
import com.knowgauge.contract.dto.PublicInvitationDto;
import com.knowgauge.contract.dto.PublicTestDto;
import com.knowgauge.contract.dto.StartInvitationAttemptInput;
import com.knowgauge.contract.dto.StartPublicAttemptInput;
import com.knowgauge.contract.dto.SubmitAttemptInput;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * What participants call (ADR009): everything is addressed by a random token,
 * never by id, and only the score is returned, never the answer key.
 */
@RequestMapping("/api/public")
@Tag(name = "Public tests", description = "Taking a test through a shared link or a personal invitation")
public interface PublicTestApi {

	@GetMapping("/tests/{testToken}")
	@Operation(summary = "Open a shared link", description = "The test behind a shared link. The link works only while the test is published.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Test returned"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed") })
	ResponseEntity<PublicTestDto> getTest(@PathVariable String testToken);

	@PostMapping("/tests/{testToken}/attempts")
	@Operation(summary = "Start from a shared link", description = "Starts an attempt for the participant. The shared link has no attempt limit.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Attempt started; returns its token and the questions"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid name or email"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed") })
	ResponseEntity<PublicAttemptDto> startFromLink(@PathVariable String testToken,
			@RequestBody @Valid StartPublicAttemptInput input);

	@GetMapping("/invitations/{invitationToken}")
	@Operation(summary = "Open a personal invitation", description = "The invited address and the test, and whether the link was already used.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitation returned"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed") })
	ResponseEntity<PublicInvitationDto> getInvitation(@PathVariable String invitationToken);

	@PostMapping("/invitations/{invitationToken}/attempt")
	@Operation(summary = "Start from a personal invitation", description = "Starts the one attempt of the invitation, with the invited address as its email. Returns the running attempt if it was already started.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt started or continued"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid name"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed"),
			@ApiResponse(responseCode = "409", description = "The invitation was already used") })
	ResponseEntity<PublicAttemptDto> startFromInvitation(@PathVariable String invitationToken,
			@RequestBody @Valid StartInvitationAttemptInput input);

	@GetMapping("/attempts/{attemptToken}")
	@Operation(summary = "Get a participant's attempt", description = "The questions while it runs (without the answer key); only the score once it is submitted.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt returned"),
			@ApiResponse(responseCode = "404", description = "Unknown attempt") })
	ResponseEntity<PublicAttemptDto> getAttempt(@PathVariable String attemptToken);

	@PostMapping("/attempts/{attemptToken}/submit")
	@Operation(summary = "Submit a participant's attempt", description = "Saves and scores the answers. Returns the score only.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt scored"),
			@ApiResponse(responseCode = "400", description = "Invalid answers"),
			@ApiResponse(responseCode = "404", description = "Unknown attempt"),
			@ApiResponse(responseCode = "409", description = "Attempt already submitted") })
	ResponseEntity<PublicAttemptDto> submit(@PathVariable String attemptToken,
			@RequestBody @Valid SubmitAttemptInput input);
}
