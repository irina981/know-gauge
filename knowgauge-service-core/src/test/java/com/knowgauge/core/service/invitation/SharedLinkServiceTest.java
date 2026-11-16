package com.knowgauge.core.service.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.AttemptConflictException;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.history.TestHistoryRecorder;

class SharedLinkServiceTest {

	private static final long TENANT = 1L;
	private static final long TEST_ID = 5L;

	private TestRepository testRepository;
	private TestHistoryRecorder history;
	private SharedLinkService service;

	@BeforeEach
	void setUp() {
		testRepository = mock(TestRepository.class);
		history = mock(TestHistoryRecorder.class);
		ExecutionContext context = mock(ExecutionContext.class);
		when(context.tenantId()).thenReturn(TENANT);
		when(context.userId()).thenReturn(OptionalLong.of(7L));
		when(testRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		service = new SharedLinkService(testRepository, history, context);
	}

	@Test
	void createsTheFirstLinkOnRequest() {
		givenTest(TestStatus.PUBLISHED, null);

		com.knowgauge.core.model.Test test = service.createOrRegenerate(TEST_ID);

		assertThat(test.getPublicToken()).isNotBlank();
		verify(history).testEvent(eq(TENANT), eq(TEST_ID), eq(TestEventType.SHARED_LINK_CREATED), eq(7L), anyMap());
	}

	@Test
	void regeneratingReplacesTheOldToken() {
		givenTest(TestStatus.PUBLISHED, "old-token");

		com.knowgauge.core.model.Test test = service.createOrRegenerate(TEST_ID);

		assertThat(test.getPublicToken()).isNotBlank().isNotEqualTo("old-token");
		verify(history).testEvent(eq(TENANT), eq(TEST_ID), eq(TestEventType.SHARED_LINK_REGENERATED), eq(7L),
				anyMap());
	}

	@Test
	void onlyPublishedTestsCanBeShared() {
		givenTest(TestStatus.REVIEWED, null);

		assertThatThrownBy(() -> service.createOrRegenerate(TEST_ID)).isInstanceOf(AttemptConflictException.class)
				.hasMessageContaining("not published");
		verify(testRepository, never()).save(any());
		verify(history, never()).testEvent(anyLong(), anyLong(), any(), any(), anyMap());
	}

	private void givenTest(TestStatus status, String token) {
		com.knowgauge.core.model.Test test = com.knowgauge.core.model.Test.builder().id(TEST_ID).tenantId(TENANT)
				.name("Safety").status(status).publicToken(token).build();
		when(testRepository.findByTenantIdAndIdForUpdate(TENANT, TEST_ID)).thenReturn(Optional.of(test));
	}
}
