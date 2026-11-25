package com.knowgauge.contract.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.knowgauge.contract.dto.CurrentUserDto;
import com.knowgauge.contract.dto.UsageSummaryDto;
import com.knowgauge.contract.dto.UserPreferencesDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The signed-in user, their settings and usage. */
@Tag(name = "Account", description = "The signed-in user, their settings and usage")
public interface AccountApi {

	@GetMapping("/api/me")
	@Operation(summary = "Get the current user", description = "The signed-in user, their tenant and roles. The first call with a new user's token creates the user and a personal tenant. With sign-in off, the fixed tenant and user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Current user returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)") })
	ResponseEntity<CurrentUserDto> me();

	@GetMapping("/api/me/preferences")
	@Operation(summary = "Get the current user's settings", description = "Settings the user chose for the app. Fields never chosen are null. With sign-in off and no stored user, all fields are null.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)") })
	ResponseEntity<UserPreferencesDto> preferences();

	@PutMapping("/api/me/preferences")
	@Operation(summary = "Replace the current user's settings", description = "Stores all settings at once; a null field goes back to the app default.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings stored"),
			@ApiResponse(responseCode = "400", description = "Unknown value, for example an unknown theme"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)"),
			@ApiResponse(responseCode = "404", description = "No stored user (sign-in off)") })
	ResponseEntity<UserPreferencesDto> updatePreferences(@RequestBody UserPreferencesDto input);

	@GetMapping("/api/me/usage")
	@Operation(summary = "Get the current workspace's usage", description = "What the user's own workspace holds (topics, documents, storage) and what it used this calendar month (UTC): tests, AI-written questions, submitted attempts and LLM tokens. The shared library is not counted.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Usage returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)") })
	ResponseEntity<UsageSummaryDto> usage();
}
