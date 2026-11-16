package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.TestInvitation;

public interface TestInvitationRepository {
	TestInvitation save(TestInvitation invitation);

	Optional<TestInvitation> findByTenantIdAndId(Long tenantId, Long id);

	Optional<TestInvitation> findByTenantIdAndPublicToken(Long tenantId, String publicToken);

	/** @param email lower-cased */
	Optional<TestInvitation> findByTenantIdAndTestIdAndEmail(Long tenantId, Long testId, String email);

	/** @return invitations of the test, oldest first */
	List<TestInvitation> findByTenantIdAndTestId(Long tenantId, Long testId);
}
