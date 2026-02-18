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
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.service.testgeneration.TestGenerationService;
import com.knowgauge.restapi.mapper.TestMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tests")
public class TestGenerationController {

	private final TestGenerationService testGenerationService;
	private final TestMapper testMapper;

	public TestGenerationController(TestGenerationService testGenerationService, TestMapper testMapper) {
		this.testGenerationService = testGenerationService;
		this.testMapper = testMapper;
		
	}

	@PostMapping("")
	public ResponseEntity<TestDto> generateTest(@RequestBody @Valid TestInput testInput) {
		Test created = testGenerationService.generate(testMapper.toDomain(testInput));

		// If Test has an ID, you can return Location header.
		// Otherwise, OK to just return 201 with body.
		URI location = tryBuildLocation("/api/tests/{id}", created);
		return (location != null) ? ResponseEntity.created(location).body(testMapper.toDto(created))
				: ResponseEntity.status(201).body(testMapper.toDto(created));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TestDto> getTestById(@PathVariable Long id) {
		return testGenerationService.getById(id).map(testMapper::toDto).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("")
	public ResponseEntity<List<TestDto>> getAllTests(@RequestParam(required = false) String status) {
		List<Test> tests = (status == null || status.isBlank()) ? testGenerationService.getAll()
				: testGenerationService.getAllByStatus(TestStatus.valueOf(status.toUpperCase()));

		return ResponseEntity.ok(tests.stream().map(testMapper::toDto).toList());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTestById(@PathVariable Long id) {
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
