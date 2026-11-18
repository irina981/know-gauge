package com.knowgauge.core.service.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.knowgauge.core.exception.ReadOnlyContentException;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.core.port.repository.TopicRepository;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.core.storage.StorageKeyBuilder;
import com.knowgauge.core.support.ContentScopes;

/**
 * Topics and documents (ADR010): a tenant reads its own content and the library's, and changes only its own.
 * Library content can be changed by an application admin. Other tenants' content behaves as if it did not exist.
 */
class TenantScopingTest {

	private static final long OWN = 5L;
	private static final long LIBRARY = 1L;
	private static final List<Long> READABLE = List.of(OWN, LIBRARY);
	private static final long FOREIGN_TOPIC = 99L;
	private static final long LIBRARY_TOPIC = 1100L;
	private static final long LIBRARY_DOC = 41L;

	private TopicRepository topicRepository;
	private DocumentRepository documentRepository;
	private StorageService storageService;
	private ExecutionContext context;

	@BeforeEach
	void setUp() {
		topicRepository = mock(TopicRepository.class);
		documentRepository = mock(DocumentRepository.class);
		storageService = mock(StorageService.class);
		context = mock(ExecutionContext.class);
		when(context.tenantId()).thenReturn(OWN);
		// Another tenant's topic is not found among the readable tenants.
		when(topicRepository.findByTenantIdInAndId(READABLE, FOREIGN_TOPIC)).thenReturn(Optional.empty());
		Topic libraryTopic = new Topic(null, "Java", null);
		libraryTopic.setId(LIBRARY_TOPIC);
		libraryTopic.setTenantId(LIBRARY);
		libraryTopic.setPath("/" + LIBRARY_TOPIC);
		when(topicRepository.findByTenantIdInAndId(READABLE, LIBRARY_TOPIC)).thenReturn(Optional.of(libraryTopic));
		Document libraryDoc = new Document();
		libraryDoc.setId(LIBRARY_DOC);
		libraryDoc.setTenantId(LIBRARY);
		libraryDoc.setStorageKey("1/41/1");
		when(documentRepository.findByTenantIdInAndId(READABLE, LIBRARY_DOC)).thenReturn(Optional.of(libraryDoc));
	}

	private TopicServiceImpl topics(boolean admin) {
		return new TopicServiceImpl(topicRepository, context, ContentScopes.withLibraries(context, admin, LIBRARY));
	}

	private DocumentServiceImpl documents(boolean admin) {
		return new DocumentServiceImpl(documentRepository, topicRepository, storageService,
				mock(StorageKeyBuilder.class), context, ContentScopes.withLibraries(context, admin, LIBRARY));
	}

	@Test
	void readsOwnAndLibraryContent() {
		topics(false).getRoots();
		topics(false).getChildren(LIBRARY_TOPIC);
		documents(false).getAllDocuments(LIBRARY_TOPIC, null);
		documents(false).countIngestedByTopic();

		verify(topicRepository).findByTenantIdInAndParentIdIsNull(READABLE);
		verify(topicRepository).findByTenantIdInAndParentId(READABLE, LIBRARY_TOPIC);
		verify(documentRepository).findByTenantIdInAndTopicId(READABLE, LIBRARY_TOPIC, null);
		verify(documentRepository).countByTopicId(eq(READABLE), any());
		assertThat(documents(false).get(LIBRARY_DOC)).isPresent();
	}

	@Test
	void descendantsStayInTheRootsTenant() {
		topics(false).getDescendants(LIBRARY_TOPIC);
		verify(topicRepository).findByTenantIdInAndPathPrefix(List.of(LIBRARY), "/" + LIBRARY_TOPIC + "/");
	}

	@Test
	void doesNotFindAnotherTenantsTopic() {
		assertThat(topics(false).get(FOREIGN_TOPIC)).isEmpty();
		assertThatThrownBy(() -> topics(false).getDescendants(FOREIGN_TOPIC))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> topics(false).create(new Topic(FOREIGN_TOPIC, "Streams", null)))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(topicRepository, never()).save(any());
	}

	@Test
	void libraryTopicsAreReadOnlyForUsers() {
		TopicServiceImpl service = topics(false);
		Topic rename = new Topic(null, "Renamed", null);
		rename.setId(LIBRARY_TOPIC);

		assertThatThrownBy(() -> service.create(new Topic(LIBRARY_TOPIC, "Streams", null)))
				.isInstanceOf(ReadOnlyContentException.class);
		assertThatThrownBy(() -> service.update(rename)).isInstanceOf(ReadOnlyContentException.class);
		assertThatThrownBy(() -> service.delete(LIBRARY_TOPIC)).isInstanceOf(ReadOnlyContentException.class);

		verify(topicRepository, never()).save(any());
		verify(topicRepository, never()).deleteByTenantIdAndId(anyLong(), anyLong());
	}

	@Test
	void applicationAdminCreatesSubtopicsInTheLibrary() {
		when(topicRepository.save(any())).thenAnswer(inv -> {
			Topic t = inv.getArgument(0);
			t.setId(1200L);
			return t;
		});

		Topic created = topics(true).create(new Topic(LIBRARY_TOPIC, "Streams", null));

		assertThat(created.getTenantId()).isEqualTo(LIBRARY);
		verify(topicRepository).existsByTenantIdAndParentIdAndName(LIBRARY, LIBRARY_TOPIC, "Streams");
		verify(topicRepository).updatePath(LIBRARY, 1200L, "/1100/1200");
	}

	@Test
	void applicationAdminDeletesLibraryTopics() {
		topics(true).delete(LIBRARY_TOPIC);
		verify(topicRepository).deleteByTenantIdAndId(LIBRARY, LIBRARY_TOPIC);
	}

	@Test
	void rootTopicsAreCreatedInTheOwnTenant() {
		when(topicRepository.save(any())).thenAnswer(inv -> {
			Topic t = inv.getArgument(0);
			t.setId(10L);
			return t;
		});

		Topic created = topics(true).create(new Topic(null, "Java", null));

		verify(topicRepository).existsByTenantIdAndParentIdIsNullAndName(OWN, "Java");
		verify(topicRepository).updatePath(OWN, 10L, "/10");
		assertThat(created.getTenantId()).isEqualTo(OWN);
	}

	@Test
	void libraryDocumentsAreReadOnlyForUsers() {
		Document upload = new Document();
		upload.setTopicId(LIBRARY_TOPIC);
		upload.setOriginalFileName("notes.pdf");

		assertThatThrownBy(() -> documents(false).uploadDocument(upload, new ByteArrayInputStream(new byte[0])))
				.isInstanceOf(ReadOnlyContentException.class);
		assertThatThrownBy(() -> documents(false).delete(LIBRARY_DOC)).isInstanceOf(ReadOnlyContentException.class);

		verify(documentRepository, never()).save(any());
		verify(documentRepository, never()).deleteByTenantIdAndId(anyLong(), anyLong());
		verify(storageService, never()).delete(any());
	}

	@Test
	void applicationAdminDeletesLibraryDocuments() {
		documents(true).delete(LIBRARY_DOC);

		verify(documentRepository).deleteByTenantIdAndId(LIBRARY, LIBRARY_DOC);
		verify(storageService).delete("1/41/1");
	}

	@Test
	void refusesToUploadIntoAnotherTenantsTopic() {
		Document document = new Document();
		document.setTopicId(FOREIGN_TOPIC);
		document.setOriginalFileName("notes.pdf");

		assertThatThrownBy(() -> documents(false).uploadDocument(document, new ByteArrayInputStream(new byte[0])))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(documentRepository, never()).save(any());
		verify(storageService, never()).put(any(), any(), anyLong(), any());
	}

	@Test
	void doesNotDeleteMissingDocuments() {
		when(documentRepository.findByTenantIdInAndId(READABLE, 7L)).thenReturn(Optional.empty());

		documents(false).delete(7L);

		verify(documentRepository, never()).deleteByTenantIdAndId(anyLong(), anyLong());
	}

	@Test
	void withoutLibraryOnlyTheOwnTenantIsRead() {
		ContentScope scope = ContentScopes.ownTenantOnly(context);
		new TopicServiceImpl(topicRepository, context, scope).getRoots();
		verify(topicRepository).findByTenantIdInAndParentIdIsNull(List.of(OWN));
	}
}
