package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.Setting;
import com.knowgauge.infra.repository.jpa.entity.SettingEntity;

@Mapper(componentModel = "spring")
public interface SettingEntityMapper {

	Setting toDomain(SettingEntity entity);

	SettingEntity toEntity(Setting domain);
}
