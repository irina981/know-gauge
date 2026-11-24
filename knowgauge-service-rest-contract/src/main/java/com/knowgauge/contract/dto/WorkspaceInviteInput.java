package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "People to invite to a workspace (ADR015); an address invited before gets its link again")
public record WorkspaceInviteInput(
		@Schema(description = "Email addresses; duplicates are merged", example = "[\"ana@example.com\"]") @NotEmpty List<String> emails,
		@Schema(description = "Optional note added to the email") @Size(max = 2000) String message) {
}
