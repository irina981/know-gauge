package com.knowgauge.restapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.SharedLinkApi;
import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.core.service.invitation.SharedLinkService;
import com.knowgauge.restapi.mapper.TestMapper;
import com.knowgauge.restapi.security.Roles;

import jakarta.annotation.security.RolesAllowed;

/**
 * The shared link of a published test (ADR009): created on request, and
 * regenerated to make the old link stop working.
 */
@RestController
public class SharedLinkController implements SharedLinkApi {

	private final SharedLinkService sharedLinkService;
	private final TestMapper testMapper;

	public SharedLinkController(SharedLinkService sharedLinkService, TestMapper testMapper) {
		this.sharedLinkService = sharedLinkService;
		this.testMapper = testMapper;
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestDto> createOrRegenerate(Long testId) {
		return ResponseEntity.ok(testMapper.toDto(sharedLinkService.createOrRegenerate(testId)));
	}
}
