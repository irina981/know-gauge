package com.knowgauge.core.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Topic extends AuditableObject {

	private Long parentId;

	private String name;

	private String description;

	private List<Topic> children;

	private String path;

}
