package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Setting;
import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.core.port.repository.SettingRepository;
import com.knowgauge.infra.repository.jpa.mapper.SettingEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.SettingJpaRepository;

@Repository
public class SettingJpaRepositoryAdapter implements SettingRepository {

	private final SettingJpaRepository jpaRepository;
	private final SettingEntityMapper mapper;

	public SettingJpaRepositoryAdapter(SettingJpaRepository jpaRepository, SettingEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public List<Setting> findSystem() {
		return jpaRepository.findByType(SettingType.SYSTEM).stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<Setting> findSystem(String key) {
		return jpaRepository.findByTypeAndKey(SettingType.SYSTEM, key).map(mapper::toDomain);
	}

	@Override
	public List<Setting> findByTenantId(Long tenantId) {
		return jpaRepository.findByTypeAndTenantId(SettingType.TENANT, tenantId).stream().map(mapper::toDomain)
				.toList();
	}

	@Override
	public List<Setting> findByTenantIdIn(Collection<Long> tenantIds) {
		if (tenantIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository.findByTypeAndTenantIdIn(SettingType.TENANT, tenantIds).stream().map(mapper::toDomain)
				.toList();
	}

	@Override
	@Transactional
	public List<Setting> saveAll(List<Setting> settings) {
		return jpaRepository.saveAll(settings.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain)
				.toList();
	}

	@Override
	@Transactional
	public void deleteAll(List<Setting> settings) {
		jpaRepository.deleteAllByIdInBatch(settings.stream().map(Setting::getId).toList());
	}

	@Override
	@Transactional
	public void deleteSystem() {
		jpaRepository.deleteByType(SettingType.SYSTEM);
	}

	@Override
	@Transactional
	public void deleteByTenantId(Long tenantId) {
		jpaRepository.deleteTenantRows(tenantId);
	}
}
