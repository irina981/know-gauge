package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Who takes the test through the shared link")
public record StartPublicAttemptInput(
		@Schema(description = "Full name", example = "Ana Petrovic") @NotBlank @Size(max = 200) String participantName,
		@Schema(description = "Email; identifies the same person across attempts", example = "ana@example.com") @NotBlank @Email @Size(max = 320) String participantEmail) {
}
