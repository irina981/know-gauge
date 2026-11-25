package com.knowgauge.restapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.PublicTestApi;
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

/**
 * What participants call (ADR009): everything is addressed by a random token,
 * never by id, and only the score is returned, never the answer key.
 */
@RestController
public class PublicTestController implements PublicTestApi {

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

	@Override
	public ResponseEntity<PublicTestDto> getTest(String testToken) {
		Test test = attemptService.getPublishedTest(testToken);
		int questionCount = questionRepository.findByTenantIdAndTestId(test.getTenantId(), test.getId()).size();
		return ResponseEntity.ok(publicMapper.toPublicTest(test, questionCount));
	}

	@Override
	public ResponseEntity<PublicAttemptDto> startFromLink(String testToken, StartPublicAttemptInput input) {
		return ResponseEntity.status(HttpStatus.CREATED).body(publicMapper.toPublicAttempt(attemptService
				.startForParticipant(testToken, input.participantName(), input.participantEmail())));
	}

	@Override
	public ResponseEntity<PublicInvitationDto> getInvitation(String invitationToken) {
		return ResponseEntity.ok(publicMapper.toPublicInvitation(invitationService.getByToken(invitationToken)));
	}

	@Override
	public ResponseEntity<PublicAttemptDto> startFromInvitation(String invitationToken,
			StartInvitationAttemptInput input) {
		return ResponseEntity.ok(publicMapper
				.toPublicAttempt(attemptService.startForInvitation(invitationToken, input.participantName())));
	}

	@Override
	public ResponseEntity<PublicAttemptDto> getAttempt(String attemptToken) {
		return ResponseEntity.ok(publicMapper.toPublicAttempt(attemptService.getByToken(attemptToken)));
	}

	@Override
	public ResponseEntity<PublicAttemptDto> submit(String attemptToken, SubmitAttemptInput input) {
		return ResponseEntity.ok(publicMapper.toPublicAttempt(
				attemptService.submitByToken(attemptToken, attemptMapper.toChosenOptions(input))));
	}
}
