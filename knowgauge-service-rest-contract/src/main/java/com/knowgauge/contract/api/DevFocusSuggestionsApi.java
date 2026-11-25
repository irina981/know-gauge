package com.knowgauge.contract.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Focus suggestions for the dev-seeded demo topics. Served only when the dev seed is enabled. */
@Tag(name = "Dev", description = "Development helpers for the seeded demo content")
public interface DevFocusSuggestionsApi {

	@GetMapping("/api/dev/focus-suggestions")
	@Operation(summary = "Focus suggestions for the demo topics", description = "Topic id to example focus texts for FOCUSED tests. The client offers the suggestions of the selected topics, their parents and their sub-topics. Only available when the dev seed is enabled.")
	ResponseEntity<Map<Long, List<String>>> getFocusSuggestions();
}
