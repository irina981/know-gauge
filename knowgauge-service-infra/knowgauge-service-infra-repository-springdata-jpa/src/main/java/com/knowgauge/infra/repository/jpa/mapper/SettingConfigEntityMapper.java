package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.SettingConfig;
import com.knowgauge.infra.repository.jpa.entity.SettingConfigEntity;

@Mapper(componentModel = "spring")
public interface SettingConfigEntityMapper {

	SettingConfig toDomain(SettingConfigEntity entity);
}
