package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Starts the attempt of a personal invitation. The email is the invited address and cannot be changed")
public record StartInvitationAttemptInput(
		@Schema(description = "Full name", example = "Ana Petrovic") @NotBlank @Size(max = 200) String participantName) {
}
