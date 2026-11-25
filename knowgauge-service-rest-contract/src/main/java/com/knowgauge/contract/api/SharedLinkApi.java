package com.knowgauge.contract.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.knowgauge.contract.dto.TestDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The shared link of a published test (ADR009): created on request, and
 * regenerated to make the old link stop working.
 */
@Tag(name = "Invitations", description = "Send personal test links by email")
public interface SharedLinkApi {

	@PostMapping("/api/tests/{testId}/shared-link")
	@Operation(summary = "Create or regenerate the shared link", description = "Creates the test's shared link, or replaces it with a new one: the old link stops working. Attempts already started can still be finished.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "The test with its new publicToken"),
			@ApiResponse(responseCode = "404", description = "Test not found"),
			@ApiResponse(responseCode = "409", description = "Test is not published") })
	ResponseEntity<TestDto> createOrRegenerate(@PathVariable Long testId);
}
