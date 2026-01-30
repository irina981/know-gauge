package com.knowgauge.repo.jpa.entity;

import java.time.Instant;

import com.knowgauge.core.model.enums.AnswerOption;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attempt_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chosen_option", nullable = false)
    private AnswerOption chosenOption;

    @Column(nullable = false)
    private Boolean correct;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = Instant.now();
    }
}
