package com.knowgauge.core.port.repository;

import java.util.Optional;

import com.knowgauge.core.model.User;

public interface UserRepository {
	User save(User user);

	Optional<User> findById(Long id);

	/** @param externalId the identity provider's subject ({@code sub}) */
	Optional<User> findByExternalId(String externalId);
}
