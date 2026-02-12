package com.knowgauge.core.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Topic extends AuditableObject {
	
	public Topic(Long parentId, String name, String description) {
		this.parentId = parentId;
		this.name = name;
		this.description = description;
	}
	
	private Long tenantId;

	private Long parentId;

	private String name;

	private String description;

	private List<Topic> children;

	private String path;

}
