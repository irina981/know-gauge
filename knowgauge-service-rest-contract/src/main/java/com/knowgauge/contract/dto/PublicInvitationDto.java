package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a personal invitation link shows before the participant starts")
public record PublicInvitationDto(
		@Schema(description = "The invited address; it becomes the attempt's email", example = "ana@example.com") String email,
		@Schema(description = "The test") PublicTestDto test,
		@Schema(description = "Empty while unused; STARTED while the attempt runs; SCORED once the link was used", example = "STARTED") String attemptStatus,
		@Schema(description = "Token of the running attempt, to continue it; only while STARTED") String attemptToken) {
}
