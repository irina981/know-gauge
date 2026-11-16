package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "Addresses to send personal links to; each gets its own link, usable once")
public record InviteInput(
		@Schema(description = "Email addresses; duplicates are merged", example = "[\"ana@example.com\", \"marko@example.com\"]") @NotEmpty List<String> emails,
		@Schema(description = "Optional note added to the email") @Size(max = 2000) String message) {
}
