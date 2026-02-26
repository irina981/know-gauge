package com.knowgauge.restapi.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.contract.dto.TestInput;
import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.service.testgeneration.TestGenerationService;
import com.knowgauge.restapi.mapper.TestMapper;
import com.knowgauge.restapi.mapper.TestQuestionMapper;

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

@RestController
@RequestMapping("/api/tests")
@Tag(name = "Test Generation", description = "Generate, retrieve, list and delete generated tests")
public class TestGenerationController {

	private final TestGenerationService testGenerationService;
	private final TestMapper testMapper;
	private final TestQuestionMapper testQuestionMapper;

	public TestGenerationController(TestGenerationService testGenerationService, TestMapper testMapper,
			TestQuestionMapper testQuestionMapper) {
		this.testGenerationService = testGenerationService;
		this.testMapper = testMapper;
		this.testQuestionMapper = testQuestionMapper;
		
	}

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
	public ResponseEntity<TestDto> generateTest(@RequestBody @Valid TestInput testInput) {
		Test created = testGenerationService.generate(testMapper.toDomain(testInput));

		// If Test has an ID, you can return Location header.
		// Otherwise, OK to just return 201 with body.
		URI location = tryBuildLocation("/api/tests/{id}", created);
		return (location != null) ? ResponseEntity.created(location).body(testMapper.toDto(created))
				: ResponseEntity.status(201).body(testMapper.toDto(created));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get test by id", description = "Returns a previously generated test for the current tenant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Test found", content = @Content(schema = @Schema(implementation = TestDto.class))),
			@ApiResponse(responseCode = "404", description = "Test not found") })
	public ResponseEntity<TestDto> getTestById(
			@Parameter(description = "Test ID", example = "1") @PathVariable Long id) {
		return testGenerationService.getById(id).map(testMapper::toDto).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/{id}/questions")
	@Operation(summary = "List questions by test id", description = "Returns generated questions for a specific test and current tenant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Questions returned", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TestQuestionDto.class)), examples = {
					@ExampleObject(name = "questions", value = """
						[
						  {
						    "id": 9001,
						    "testId": 5001,
						    "questionIndex": 1,
						    "questionText": "Which Spring Boot annotation marks the main application class?",
						    "optionA": "@SpringBootApplication",
						    "optionB": "@EnableAutoConfig",
						    "optionC": "@BootApp",
						    "optionD": "@SpringApp",
						    "correctOptions": ["A"],
						    "explanation": "@SpringBootApplication combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.",
						    "sourceChunkIdsJson": [10101, 10102]
						  }
						]
						""")
			})),
			@ApiResponse(responseCode = "404", description = "Test not found") })
	public ResponseEntity<List<TestQuestionDto>> getTestQuestionsByTestId(
			@Parameter(description = "Test ID", example = "1") @PathVariable Long id) {
		return testGenerationService.getQuestionsByTestId(id)
				.map(questions -> ResponseEntity.ok(questions.stream().map(testQuestionMapper::toDto).toList()))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("")
	@Operation(summary = "List tests", description = "Returns all tests for the current tenant. Optionally filter by status.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Tests returned") })
	public ResponseEntity<List<TestDto>> getAllTests(
			@Parameter(description = "Optional status filter", example = "GENERATED") @RequestParam(required = false) String status) {
		List<Test> tests = (status == null || status.isBlank()) ? testGenerationService.getAll()
				: testGenerationService.getAllByStatus(TestStatus.valueOf(status.toUpperCase()));

		return ResponseEntity.ok(tests.stream().map(testMapper::toDto).toList());
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete test", description = "Deletes a test and associated generated questions for the current tenant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Test deleted") })
	public ResponseEntity<Void> deleteTestById(
			@Parameter(description = "Test ID", example = "1") @PathVariable Long id) {
		testGenerationService.deleteById(id);
		return ResponseEntity.status(200).build();
	}
	
	/**
	 * Best-effort Location header creator. Works if your domain objects expose
	 * getId(). If your Topic/Document uses a different ID getter, adjust this
	 * method.
	 */
	private URI tryBuildLocation(String pathTemplate, Object entity) {
		try {
			var getId = entity.getClass().getMethod("getId");
			Object id = getId.invoke(entity);
			if (id == null)
				return null;

			return ServletUriComponentsBuilder.fromCurrentContextPath().path(pathTemplate).buildAndExpand(id).toUri();
		} catch (Exception ignored) {
			return null;
		}
	}

}
