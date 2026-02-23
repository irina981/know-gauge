package com.knowgauge.infra.repository.jpa.entity;

import java.util.List;

import com.knowgauge.core.model.enums.AnswerOption;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "attempt_answers")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
public class AttemptAnswerEntity extends AuditableEntity {
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attempt_answer_chosen_options", joinColumns = @JoinColumn(name = "attempt_answer_id"))
    @Column(name = "chosen_option", nullable = false)
    @Enumerated(EnumType.STRING)
    private List<AnswerOption> chosenOptions;

    @Column(nullable = false)
    private Boolean correct;
}
