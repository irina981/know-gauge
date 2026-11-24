package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.SystemSetting;
import com.knowgauge.infra.repository.jpa.entity.SystemSettingEntity;

@Mapper(componentModel = "spring")
public interface SystemSettingEntityMapper {

	SystemSetting toDomain(SystemSettingEntity entity);

	SystemSettingEntity toEntity(SystemSetting domain);
}
