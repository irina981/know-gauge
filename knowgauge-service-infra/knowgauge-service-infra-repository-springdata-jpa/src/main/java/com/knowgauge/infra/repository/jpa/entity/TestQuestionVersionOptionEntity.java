package com.knowgauge.infra.repository.jpa.entity;

import org.hibernate.annotations.Immutable;

import com.knowgauge.core.model.enums.OptionLetter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One option of a question version (immutable, like its version).
 */
@Entity
@Immutable
@Table(name = "test_question_version_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestionVersionOptionEntity extends PersistedEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "version_id", nullable = false)
	private TestQuestionVersionEntity version;

	@Enumerated(EnumType.STRING)
	@Column(name = "option_letter", nullable = false, length = 1)
	private OptionLetter letter;

	@Column(name = "text", nullable = false, columnDefinition = "TEXT")
	private String text;

	@Column(name = "correct", nullable = false)
	private boolean correct;

	@Column(name = "flagged", nullable = false)
	private boolean flagged;

	@Column(name = "suggested_text", columnDefinition = "TEXT")
	private String suggestedText;

	@Column(name = "flag_reason", columnDefinition = "TEXT")
	private String flagReason;
}
