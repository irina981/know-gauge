package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.infra.repository.jpa.entity.TestInvitationEntity;

@Mapper(componentModel = "spring")
public interface TestInvitationEntityMapper {
	TestInvitation toDomain(TestInvitationEntity entity);

	TestInvitationEntity toEntity(TestInvitation domain);
}
