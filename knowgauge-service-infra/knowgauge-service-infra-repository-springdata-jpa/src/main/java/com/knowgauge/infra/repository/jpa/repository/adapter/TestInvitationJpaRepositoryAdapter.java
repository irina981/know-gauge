package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.port.repository.TestInvitationRepository;
import com.knowgauge.infra.repository.jpa.mapper.TestInvitationEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TestInvitationJpaRepository;

@Repository
public class TestInvitationJpaRepositoryAdapter implements TestInvitationRepository {

	private final TestInvitationJpaRepository jpaRepository;
	private final TestInvitationEntityMapper mapper;

	public TestInvitationJpaRepositoryAdapter(TestInvitationJpaRepository jpaRepository,
			TestInvitationEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public TestInvitation save(TestInvitation invitation) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(invitation)));
	}

	@Override
	public Optional<TestInvitation> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public Optional<TestInvitation> findByPublicToken(String publicToken) {
		return jpaRepository.findByPublicToken(publicToken).map(mapper::toDomain);
	}

	@Override
	public Optional<TestInvitation> findByTenantIdAndTestIdAndEmail(Long tenantId, Long testId, String email) {
		return jpaRepository.findByTenantIdAndTestIdAndEmail(tenantId, testId, email).map(mapper::toDomain);
	}

	@Override
	public List<TestInvitation> findByTenantIdAndTestId(Long tenantId, Long testId) {
		return jpaRepository.findByTenantIdAndTestIdOrderByCreatedAtAscIdAsc(tenantId, testId).stream()
				.map(mapper::toDomain).toList();
	}
}
