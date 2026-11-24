package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.SettingConfig;
import com.knowgauge.core.port.repository.SettingConfigRepository;
import com.knowgauge.infra.repository.jpa.mapper.SettingConfigEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.SettingConfigJpaRepository;

@Repository
public class SettingConfigJpaRepositoryAdapter implements SettingConfigRepository {

	private final SettingConfigJpaRepository jpaRepository;
	private final SettingConfigEntityMapper mapper;

	public SettingConfigJpaRepositoryAdapter(SettingConfigJpaRepository jpaRepository,
			SettingConfigEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SettingConfig> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}
}
