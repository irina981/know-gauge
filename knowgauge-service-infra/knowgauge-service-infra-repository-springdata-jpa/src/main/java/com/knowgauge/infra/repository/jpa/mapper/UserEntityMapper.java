package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.User;
import com.knowgauge.infra.repository.jpa.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
	User toDomain(UserEntity entity);

	UserEntity toEntity(User domain);
}
