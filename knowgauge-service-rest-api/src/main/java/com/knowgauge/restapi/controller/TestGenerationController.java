package com.knowgauge.restapi.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.knowgauge.contract.api.TestGenerationApi;
import com.knowgauge.contract.dto.TestDefaultsDto;
import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.contract.dto.TestInput;
import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.service.testgeneration.TestGenerationService;
import com.knowgauge.restapi.mapper.TestMapper;
import com.knowgauge.restapi.mapper.TestQuestionMapper;
import com.knowgauge.restapi.security.Roles;

import jakarta.annotation.security.RolesAllowed;

@RestController
public class TestGenerationController implements TestGenerationApi {

	private final TestGenerationService testGenerationService;
	private final TestMapper testMapper;
	private final TestQuestionMapper testQuestionMapper;

	public TestGenerationController(TestGenerationService testGenerationService, TestMapper testMapper,
			TestQuestionMapper testQuestionMapper) {
		this.testGenerationService = testGenerationService;
		this.testMapper = testMapper;
		this.testQuestionMapper = testQuestionMapper;

	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestDto> generateTest(TestInput testInput) {
		Test created = testGenerationService.generate(testMapper.toDomain(testInput));

		// If Test has an ID, you can return Location header.
		// Otherwise, OK to just return 201 with body.
		URI location = tryBuildLocation("/api/tests/{id}", created);
		return (location != null) ? ResponseEntity.created(location).body(testMapper.toDto(created))
				: ResponseEntity.status(201).body(testMapper.toDto(created));
	}

	@Override
	public ResponseEntity<TestDefaultsDto> getTestDefaults() {
		Test d = testGenerationService.defaults();
		return ResponseEntity.ok(new TestDefaultsDto(d.getQuestionCount(), name(d.getDifficulty()),
				name(d.getAnswerCardinality()), name(d.getCoverageMode()), name(d.getLanguage()),
				d.getGenerationModel()));
	}

	private static String name(Enum<?> value) {
		return value == null ? null : value.name();
	}

	@Override
	public ResponseEntity<TestDto> getTestById(Long id) {
		return testGenerationService.getById(id).map(testMapper::toDto).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<List<TestQuestionDto>> getTestQuestionsByTestId(Long id) {
		return testGenerationService.getQuestionsByTestId(id)
				.map(questions -> ResponseEntity.ok(questions.stream().map(testQuestionMapper::toDto).toList()))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<List<TestDto>> getAllTests(String status) {
		List<Test> tests = (status == null || status.isBlank()) ? testGenerationService.getAll()
				: testGenerationService.getAllByStatus(TestStatus.valueOf(status.toUpperCase()));

		return ResponseEntity.ok(tests.stream().map(testMapper::toDto).toList());
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<Void> deleteTestById(Long id) {
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
