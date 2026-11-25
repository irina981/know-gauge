package com.knowgauge.restapi.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.knowgauge.contract.api.AttemptApi;
import com.knowgauge.contract.dto.AttemptDetailDto;
import com.knowgauge.contract.dto.AttemptDto;
import com.knowgauge.contract.dto.SubmitAttemptInput;
import com.knowgauge.core.service.attempt.AttemptService;
import com.knowgauge.core.service.attempt.AttemptService.AttemptView;
import com.knowgauge.restapi.mapper.AttemptMapper;
import com.knowgauge.restapi.security.Roles;

import jakarta.annotation.security.RolesAllowed;

/**
 * Taking published tests: start, submit (scored at once) and list attempts.
 */
@RestController
public class AttemptController implements AttemptApi {

	private final AttemptService attemptService;
	private final AttemptMapper attemptMapper;

	public AttemptController(AttemptService attemptService, AttemptMapper attemptMapper) {
		this.attemptService = attemptService;
		this.attemptMapper = attemptMapper;
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<AttemptDetailDto> start(Long testId) {
		AttemptView view = attemptService.start(testId);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/attempts/{id}")
				.buildAndExpand(view.attempt().getId()).toUri();
		return ResponseEntity.created(location).body(attemptMapper.toDetailDto(view));
	}

	@Override
	public ResponseEntity<AttemptDetailDto> get(Long attemptId) {
		return ResponseEntity.ok(attemptMapper.toDetailDto(attemptService.get(attemptId)));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<AttemptDetailDto> submit(Long attemptId, SubmitAttemptInput input) {
		return ResponseEntity.ok(
				attemptMapper.toDetailDto(attemptService.submit(attemptId, attemptMapper.toChosenOptions(input))));
	}

	@Override
	public ResponseEntity<List<AttemptDto>> list(Long testId, Long userId, String participantEmail) {
		return ResponseEntity.ok(attemptService.list(testId, userId, participantEmail).stream().map(attemptMapper::toDto).toList());
	}
}
