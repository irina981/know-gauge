package com.knowgauge.contract.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.knowgauge.contract.dto.TestDefaultsDto;
import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.contract.dto.TestInput;
import com.knowgauge.contract.dto.TestQuestionDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Generating tests and reading them. */
@RequestMapping("/api/tests")
@Tag(name = "Test Generation", description = "Generate, retrieve, list and delete generated tests")
public interface TestGenerationApi {

	@PostMapping("")
	@Operation(summary = "Generate a new test", description = "Creates a new test draft, runs generation, validates questions and returns the generated test metadata.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Test generated successfully", content = @Content(schema = @Schema(implementation = TestDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload"),
			@ApiResponse(responseCode = "500", description = "Generation failed") })
	@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Test generation input", content = @Content(mediaType = "application/json", examples = {
			@ExampleObject(name = "basic", value = """
					{
					  "topicIds": [],
					  "documentIds": [1],
					  "difficulty": "MEDIUM",
					  "questionCount": 10,
					  "answerCardinality": "SINGLE_CORRECT",
					  "language": "EN",
					  "generationParams": {
					    "temperature": 0.2,
					    "maxOutputTokens": 1200
					  }
					}
					""")
	}))
	ResponseEntity<TestDto> generateTest(@RequestBody @Valid TestInput testInput);

	@GetMapping("/defaults")
	@Operation(summary = "Get the defaults of a new test", description = "What POST /api/tests uses for each field the request leaves empty, for the current workspace: its own configuration (ADR014), else the system values. The Generate test form starts from these.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Defaults returned") })
	ResponseEntity<TestDefaultsDto> getTestDefaults();

	@GetMapping("/{id}")
	@Operation(summary = "Get test by id", description = "Returns a previously generated test for the current tenant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Test found", content = @Content(schema = @Schema(implementation = TestDto.class))),
			@ApiResponse(responseCode = "404", description = "Test not found") })
	ResponseEntity<TestDto> getTestById(@Parameter(description = "Test ID", example = "1") @PathVariable Long id);

	@GetMapping("/{id}/questions")
	@Operation(summary = "List questions by test id", description = "Returns the questions of a test (current version of each) for the current tenant. Questions with status NEEDS_REVIEW have flagged options with suggested fixes.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Questions returned", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TestQuestionDto.class)), examples = {
					@ExampleObject(name = "questions", value = """
						[
						  {
						    "id": 9001,
						    "testId": 5001,
						    "questionIndex": 0,
						    "status": "NEEDS_REVIEW",
						    "currentVersionId": 12001,
						    "versionNo": 1,
						    "origin": "GENERATED",
						    "questionText": "Which Spring Boot annotation marks the main application class?",
						    "options": {
						      "A": { "letter": "A", "text": "@SpringBootApplication", "correct": true, "flagged": false, "suggestedText": null, "flagReason": null },
						      "B": { "letter": "B", "text": "@EnableAutoConfiguration", "correct": false, "flagged": true, "suggestedText": "@EnableScheduling", "flagReason": "Also implied by @SpringBootApplication on the main class" },
						      "C": { "letter": "C", "text": "@BootApp", "correct": false, "flagged": false, "suggestedText": null, "flagReason": null },
						      "D": { "letter": "D", "text": "@SpringApp", "correct": false, "flagged": false, "suggestedText": null, "flagReason": null }
						    },
						    "correctOptions": ["A"],
						    "flaggedOptions": ["B"],
						    "explanation": "@SpringBootApplication combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.",
						    "sourceChunkIds": [10101, 10102]
						  }
						]
						""")
			})),
			@ApiResponse(responseCode = "404", description = "Test not found") })
	ResponseEntity<List<TestQuestionDto>> getTestQuestionsByTestId(
			@Parameter(description = "Test ID", example = "1") @PathVariable Long id);

	@GetMapping("")
	@Operation(summary = "List tests", description = "Returns all tests for the current tenant. Optionally filter by status.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Tests returned") })
	ResponseEntity<List<TestDto>> getAllTests(
			@Parameter(description = "Optional status filter", example = "GENERATED") @RequestParam(required = false) String status);

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete test", description = "Deletes a test and associated generated questions for the current tenant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Test deleted") })
	ResponseEntity<Void> deleteTestById(@Parameter(description = "Test ID", example = "1") @PathVariable Long id);
}
