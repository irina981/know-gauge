package com.knowgauge.core.service.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.PermissionDeniedException;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.content.ContentScope;

/** What each role may do in its workspace (ADR017). */
class RolePermissionsTest {

	private static final long TENANT = 3L;
	private static final long ME = 10L;
	private static final long COLLEAGUE = 11L;
	private static final long MY_TEST = 100L;
	private static final long THEIR_TEST = 101L;

	private final ExecutionContext context = mock(ExecutionContext.class);
	private final TestRepository tests = mock(TestRepository.class);

	private RolePermissions as(Role... roles) {
		Optional<Set<Role>> current = roles.length == 0 ? Optional.of(Set.of())
				: Optional.of(EnumSet.copyOf(List.of(roles)));
		return permissions(current);
	}

	private RolePermissions permissions(Optional<Set<Role>> roles) {
		when(context.tenantId()).thenReturn(TENANT);
		when(context.userId()).thenReturn(OptionalLong.of(ME));
		when(tests.findByTenantIdAndId(TENANT, MY_TEST)).thenReturn(Optional.of(test(MY_TEST, ME)));
		when(tests.findByTenantIdAndId(TENANT, THEIR_TEST)).thenReturn(Optional.of(test(THEIR_TEST, COLLEAGUE)));
		return new RolePermissions(() -> roles, context, tests);
	}

	private static com.knowgauge.core.model.Test test(long id, long createdBy) {
		com.knowgauge.core.model.Test test = com.knowgauge.core.model.Test.builder().id(id).tenantId(TENANT).build();
		test.setCreatedBy(createdBy);
		return test;
	}

	@Test
	void aTenantAdminDoesEverything() {
		RolePermissions admin = as(Role.TENANT_ADMIN);

		assertThat(admin.canChangeLibrary()).isTrue();
		assertThat(admin.canCreateTests()).isTrue();
		assertThatCode(() -> admin.requireChangeTest(THEIR_TEST)).doesNotThrowAnyException();
		assertThatCode(admin::requireTakeTests).doesNotThrowAnyException();
	}

	@Test
	void anApplicationAdminDoesEverythingToo() {
		RolePermissions admin = as(Role.ADMIN);

		assertThat(admin.canChangeLibrary()).isTrue();
		assertThatCode(() -> admin.requireChangeTest(THEIR_TEST)).doesNotThrowAnyException();
	}

	@Test
	void aUserCreatesAndTakesTestsAndChangesOnlyTheirOwn() {
		RolePermissions user = as(Role.TENANT_USER);

		assertThat(user.canChangeLibrary()).isFalse();
		assertThatThrownBy(user::requireChangeLibrary).isInstanceOf(PermissionDeniedException.class);
		assertThatCode(user::requireCreateTests).doesNotThrowAnyException();
		assertThatCode(user::requireTakeTests).doesNotThrowAnyException();
		assertThatCode(() -> user.requireChangeTest(MY_TEST)).doesNotThrowAnyException();
		assertThatThrownBy(() -> user.requireChangeTest(THEIR_TEST)).isInstanceOf(PermissionDeniedException.class)
				.hasMessageContaining("only the tests you created");
	}

	@Test
	void aViewerOnlyReads() {
		RolePermissions viewer = as(Role.TENANT_VIEWER);

		assertThat(viewer.canChangeLibrary()).isFalse();
		assertThat(viewer.canCreateTests()).isFalse();
		assertThatThrownBy(viewer::requireCreateTests).isInstanceOf(PermissionDeniedException.class);
		assertThatThrownBy(viewer::requireTakeTests).isInstanceOf(PermissionDeniedException.class);
		assertThatThrownBy(() -> viewer.requireChangeTest(MY_TEST)).isInstanceOf(PermissionDeniedException.class);
	}

	@Test
	void aUserWithoutAnyRoleChangesNothing() {
		RolePermissions none = as();

		assertThat(none.canChangeLibrary()).isFalse();
		assertThat(none.canCreateTests()).isFalse();
		assertThatThrownBy(() -> none.requireChangeTest(MY_TEST)).isInstanceOf(PermissionDeniedException.class);
	}

	@Test
	void workWithoutASignedInUserIsNotRestricted() {
		// Scheduled jobs, startup seeding and security mode fixed.
		RolePermissions system = permissions(Optional.empty());

		assertThat(system.canChangeLibrary()).isTrue();
		assertThat(system.canCreateTests()).isTrue();
		assertThatCode(() -> system.requireChangeTest(THEIR_TEST)).doesNotThrowAnyException();
	}

	@Test
	void anUnknownTestIsLeftToTheServiceToReport() {
		RolePermissions user = as(Role.TENANT_USER);

		assertThatCode(() -> user.requireChangeTest(999L)).doesNotThrowAnyException();
	}

	@Test
	void theLibraryOfTheOwnWorkspaceIsReadOnlyForUsersAndViewers() {
		when(context.tenantId()).thenReturn(TENANT);
		TenantRepository tenants = mock(TenantRepository.class);
		when(tenants.findIdsByType(TenantType.LIBRARY)).thenReturn(List.of());
		ContentScope forUser = new ContentScope(tenants, context, () -> false, () -> false,
				() -> Optional.of(Set.of(Role.TENANT_USER)));
		ContentScope forAdmin = new ContentScope(tenants, context, () -> false, () -> false,
				() -> Optional.of(Set.of(Role.TENANT_ADMIN)));

		assertThat(forUser.canWrite(TENANT)).isFalse();
		assertThatThrownBy(() -> forUser.requireWritable(TENANT, "Topic 5"))
				.isInstanceOf(PermissionDeniedException.class).hasMessageContaining("admin of this workspace");
		assertThat(forAdmin.canWrite(TENANT)).isTrue();
	}
}
