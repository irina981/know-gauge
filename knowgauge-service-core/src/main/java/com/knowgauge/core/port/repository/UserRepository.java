package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.User;

public interface UserRepository {
	User save(User user);

	Optional<User> findById(Long id);

	/** @param externalId the identity provider's subject ({@code sub}) */
	Optional<User> findByExternalId(String externalId);

	/** @return the users whose one tenant is the given one */
	List<User> findByTenantId(Long tenantId);
}
