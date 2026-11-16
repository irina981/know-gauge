package com.knowgauge.restapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.PublicAttemptDto;
import com.knowgauge.contract.dto.PublicInvitationDto;
import com.knowgauge.contract.dto.PublicTestDto;
import com.knowgauge.contract.dto.StartInvitationAttemptInput;
import com.knowgauge.contract.dto.StartPublicAttemptInput;
import com.knowgauge.contract.dto.SubmitAttemptInput;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.service.attempt.AttemptService;
import com.knowgauge.core.service.invitation.InvitationService;
import com.knowgauge.restapi.mapper.AttemptMapper;
import com.knowgauge.restapi.mapper.PublicMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * What participants call (ADR009): everything is addressed by a random token,
 * never by id, and only the score is returned, never the answer key.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public tests", description = "Taking a test through a shared link or a personal invitation")
public class PublicTestController {

	private final AttemptService attemptService;
	private final InvitationService invitationService;
	private final TestQuestionRepository questionRepository;
	private final PublicMapper publicMapper;
	private final AttemptMapper attemptMapper;

	public PublicTestController(AttemptService attemptService, InvitationService invitationService,
			TestQuestionRepository questionRepository, PublicMapper publicMapper, AttemptMapper attemptMapper) {
		this.attemptService = attemptService;
		this.invitationService = invitationService;
		this.questionRepository = questionRepository;
		this.publicMapper = publicMapper;
		this.attemptMapper = attemptMapper;
	}

	@GetMapping("/tests/{testToken}")
	@Operation(summary = "Open a shared link", description = "The test behind a shared link. The link works only while the test is published.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Test returned"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed") })
	public ResponseEntity<PublicTestDto> getTest(@PathVariable String testToken) {
		Test test = attemptService.getPublishedTest(testToken);
		int questionCount = questionRepository.findByTenantIdAndTestId(test.getTenantId(), test.getId()).size();
		return ResponseEntity.ok(publicMapper.toPublicTest(test, questionCount));
	}

	@PostMapping("/tests/{testToken}/attempts")
	@Operation(summary = "Start from a shared link", description = "Starts an attempt for the participant. The shared link has no attempt limit.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Attempt started; returns its token and the questions"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid name or email"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed") })
	public ResponseEntity<PublicAttemptDto> startFromLink(@PathVariable String testToken,
			@RequestBody @Valid StartPublicAttemptInput input) {
		return ResponseEntity.status(HttpStatus.CREATED).body(publicMapper.toPublicAttempt(attemptService
				.startForParticipant(testToken, input.participantName(), input.participantEmail())));
	}

	@GetMapping("/invitations/{invitationToken}")
	@Operation(summary = "Open a personal invitation", description = "The invited address and the test, and whether the link was already used.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitation returned"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed") })
	public ResponseEntity<PublicInvitationDto> getInvitation(@PathVariable String invitationToken) {
		return ResponseEntity.ok(publicMapper.toPublicInvitation(invitationService.getByToken(invitationToken)));
	}

	@PostMapping("/invitations/{invitationToken}/attempt")
	@Operation(summary = "Start from a personal invitation", description = "Starts the one attempt of the invitation, with the invited address as its email. Returns the running attempt if it was already started.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt started or continued"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid name"),
			@ApiResponse(responseCode = "404", description = "Unknown link or the test is closed"),
			@ApiResponse(responseCode = "409", description = "The invitation was already used") })
	public ResponseEntity<PublicAttemptDto> startFromInvitation(@PathVariable String invitationToken,
			@RequestBody @Valid StartInvitationAttemptInput input) {
		return ResponseEntity.ok(publicMapper
				.toPublicAttempt(attemptService.startForInvitation(invitationToken, input.participantName())));
	}

	@GetMapping("/attempts/{attemptToken}")
	@Operation(summary = "Get a participant's attempt", description = "The questions while it runs (without the answer key); only the score once it is submitted.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt returned"),
			@ApiResponse(responseCode = "404", description = "Unknown attempt") })
	public ResponseEntity<PublicAttemptDto> getAttempt(@PathVariable String attemptToken) {
		return ResponseEntity.ok(publicMapper.toPublicAttempt(attemptService.getByToken(attemptToken)));
	}

	@PostMapping("/attempts/{attemptToken}/submit")
	@Operation(summary = "Submit a participant's attempt", description = "Saves and scores the answers. Returns the score only.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Attempt scored"),
			@ApiResponse(responseCode = "400", description = "Invalid answers"),
			@ApiResponse(responseCode = "404", description = "Unknown attempt"),
			@ApiResponse(responseCode = "409", description = "Attempt already submitted") })
	public ResponseEntity<PublicAttemptDto> submit(@PathVariable String attemptToken,
			@RequestBody @Valid SubmitAttemptInput input) {
		return ResponseEntity.ok(publicMapper.toPublicAttempt(
				attemptService.submitByToken(attemptToken, attemptMapper.toChosenOptions(input))));
	}
}
