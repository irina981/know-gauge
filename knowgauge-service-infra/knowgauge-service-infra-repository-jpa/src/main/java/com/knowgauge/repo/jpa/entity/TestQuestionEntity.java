package com.knowgauge.repo.jpa.entity;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.Type;

import com.knowgauge.core.model.enums.AnswerOption;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "test_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestionEntity  extends AuditableEntity  {

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "question_index", nullable = false)
    private Integer questionIndex;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false)
    private String optionC;

    @Column(name = "option_d", nullable = false)
    private String optionD;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_option", nullable = false)
    private AnswerOption correctOption;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "source_chunk_ids_json", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private List<Long> sourceChunkIdsJson;
}
