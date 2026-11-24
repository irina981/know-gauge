package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.SystemSetting;
import com.knowgauge.core.port.repository.SystemSettingRepository;
import com.knowgauge.infra.repository.jpa.mapper.SystemSettingEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.SystemSettingJpaRepository;

@Repository
public class SystemSettingJpaRepositoryAdapter implements SystemSettingRepository {

	private final SystemSettingJpaRepository jpaRepository;
	private final SystemSettingEntityMapper mapper;

	public SystemSettingJpaRepositoryAdapter(SystemSettingJpaRepository jpaRepository,
			SystemSettingEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public List<SystemSetting> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<SystemSetting> findByKey(String key) {
		return jpaRepository.findByKey(key).map(mapper::toDomain);
	}

	@Override
	public SystemSetting save(SystemSetting setting) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(setting)));
	}

	@Override
	public List<SystemSetting> saveAll(List<SystemSetting> settings) {
		return jpaRepository.saveAll(settings.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain)
				.toList();
	}

	@Override
	public void deleteAll() {
		jpaRepository.deleteAllInBatch();
	}

	@Override
	public void delete(SystemSetting setting) {
		jpaRepository.deleteById(setting.getId());
	}
}
