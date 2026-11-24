package com.knowgauge.core.service.content;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.StorageLimitReachedException;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.core.port.repository.TopicRepository;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.core.port.storage.StorageService.StoredObject;
import com.knowgauge.core.model.WorkspaceLimits;
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.core.storage.StorageKeyBuilder;
import com.knowgauge.core.support.ContentScopes;

/** An upload that would take a workspace over its storage limit is refused. */
class DocumentStorageLimitTest {

	private static final long OWN = 5L;
	private static final long LIBRARY = 1L;
	private static final List<Long> READABLE = List.of(OWN, LIBRARY);
	private static final long OWN_TOPIC = 10L;
	private static final long LIBRARY_TOPIC = 1100L;

	private final TopicRepository topicRepository = mock(TopicRepository.class);
	private final DocumentRepository documentRepository = mock(DocumentRepository.class);
	private final StorageService storageService = mock(StorageService.class);
	private final TenantSettingsService settings = mock(TenantSettingsService.class);
	private final ExecutionContext context = mock(ExecutionContext.class);

	@BeforeEach
	void setUp() {
		when(context.tenantId()).thenReturn(OWN);
		when(topicRepository.findByTenantIdInAndId(READABLE, OWN_TOPIC)).thenReturn(Optional.of(topic(OWN_TOPIC, OWN)));
		when(topicRepository.findByTenantIdInAndId(READABLE, LIBRARY_TOPIC))
				.thenReturn(Optional.of(topic(LIBRARY_TOPIC, LIBRARY)));
		when(documentRepository.save(any())).thenAnswer(inv -> {
			Document saved = inv.getArgument(0);
			saved.setId(7L);
			return saved;
		});
		when(storageService.put(any(), any(), anyLong(), any()))
				.thenReturn(new StoredObject("key", "etag", null, 0, "application/pdf"));
		when(settings.limits(OWN)).thenReturn(new WorkspaceLimits(null, 1_000L, null));
		// The library's own value 0: no limit (ADR014).
		when(settings.limits(LIBRARY)).thenReturn(WorkspaceLimits.NONE);
		when(documentRepository.sumFileSizeBytesByTenantId(OWN)).thenReturn(900L);
		when(documentRepository.sumFileSizeBytesByTenantId(LIBRARY)).thenReturn(5_000L);
	}

	@Test
	void anUploadOverTheLimitIsRefusedBeforeAnythingIsStored() {
		assertThatThrownBy(() -> service(false).uploadDocument(document(OWN_TOPIC, 101L), empty()))
				.isInstanceOf(StorageLimitReachedException.class).hasMessageContaining("does not fit");
		verify(documentRepository, never()).save(any());
		verify(storageService, never()).put(any(), any(), anyLong(), any());
	}

	@Test
	void anUploadThatFillsTheLimitExactlyIsStored() {
		service(false).uploadDocument(document(OWN_TOPIC, 100L), empty());
		verify(storageService).put(any(), any(), anyLong(), any());
	}

	@Test
	void theLibraryHasNoLimit() {
		service(true).uploadDocument(document(LIBRARY_TOPIC, 100L), empty());
		verify(storageService).put(any(), any(), anyLong(), any());
	}

	@Test
	void withoutALimitEveryUploadIsStored() {
		when(settings.limits(OWN)).thenReturn(WorkspaceLimits.NONE);
		service(false).uploadDocument(document(OWN_TOPIC, 1_000_000L), empty());
		verify(storageService).put(any(), any(), anyLong(), any());
	}

	private DocumentServiceImpl service(boolean admin) {
		return new DocumentServiceImpl(documentRepository, topicRepository, storageService,
				mock(StorageKeyBuilder.class), context, ContentScopes.withLibraries(context, admin, LIBRARY), settings);
	}

	private static Topic topic(long id, long tenantId) {
		Topic topic = new Topic(null, "Java", null);
		topic.setId(id);
		topic.setTenantId(tenantId);
		topic.setPath("/" + id);
		return topic;
	}

	private static Document document(long topicId, long size) {
		Document document = new Document();
		document.setTopicId(topicId);
		document.setOriginalFileName("a.pdf");
		document.setChecksum("abc");
		document.setFileSizeBytes(size);
		document.setContentType("application/pdf");
		return document;
	}

	private static ByteArrayInputStream empty() {
		return new ByteArrayInputStream(new byte[0]);
	}
}
