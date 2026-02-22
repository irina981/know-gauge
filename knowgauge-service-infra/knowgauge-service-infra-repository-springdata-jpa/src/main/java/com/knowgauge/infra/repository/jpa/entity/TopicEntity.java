package com.knowgauge.infra.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "topics")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TopicEntity extends AuditableEntity {
	@Column(name = "tenant_id", nullable = false)
    private Long tenantId;
	
    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String path;


}
