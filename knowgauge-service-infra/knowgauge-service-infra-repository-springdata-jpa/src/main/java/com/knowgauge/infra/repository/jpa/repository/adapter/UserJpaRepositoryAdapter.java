package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.User;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.infra.repository.jpa.mapper.UserEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.UserJpaRepository;

@Repository
public class UserJpaRepositoryAdapter implements UserRepository {

	private final UserJpaRepository jpaRepository;
	private final UserEntityMapper mapper;

	public UserJpaRepositoryAdapter(UserJpaRepository jpaRepository, UserEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public User save(User user) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
	}

	@Override
	public Optional<User> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<User> findByExternalId(String externalId) {
		return jpaRepository.findByExternalId(externalId).map(mapper::toDomain);
	}

	@Override
	public List<User> findByTenantId(Long tenantId) {
		return jpaRepository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
	}
}
