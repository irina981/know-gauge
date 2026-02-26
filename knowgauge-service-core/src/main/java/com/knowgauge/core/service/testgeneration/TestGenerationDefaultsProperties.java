package com.knowgauge.core.service.testgeneration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.Language;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;

@Component
@ConfigurationProperties(prefix = "kg.testgen.defaults")
public class TestGenerationDefaultsProperties {

	private TestDifficulty difficulty;
	private Boolean avoidRepeats;
	private TestCoverageMode coverageMode;
	private Integer questionCount;
	private AnswerCardinality answerCardinality;
	private Language language;
	private String promptTemplateId;
	private String generationModel;
	private Integer chunksPerQuestion;
	private Integer minChunksPerTest;
	private Integer questionGenerationBatchSize;
	private Integer minMultipleCorrectPercentage;
	private Integer maxConsecutiveZeroProgressBatches;

	public TestDifficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(TestDifficulty difficulty) {
		this.difficulty = difficulty;
	}

	public Boolean getAvoidRepeats() {
		return avoidRepeats;
	}

	public void setAvoidRepeats(Boolean avoidRepeats) {
		this.avoidRepeats = avoidRepeats;
	}

	public TestCoverageMode getCoverageMode() {
		return coverageMode;
	}

	public void setCoverageMode(TestCoverageMode coverageMode) {
		this.coverageMode = coverageMode;
	}

	public Integer getQuestionCount() {
		return questionCount;
	}

	public void setQuestionCount(Integer questionCount) {
		this.questionCount = questionCount;
	}

	public AnswerCardinality getAnswerCardinality() {
		return answerCardinality;
	}

	public void setAnswerCardinality(AnswerCardinality answerCardinality) {
		this.answerCardinality = answerCardinality;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public String getPromptTemplateId() {
		return promptTemplateId;
	}

	public void setPromptTemplateId(String promptTemplateId) {
		this.promptTemplateId = promptTemplateId;
	}

	public String getGenerationModel() {
		return generationModel;
	}

	public void setGenerationModel(String generationModel) {
		this.generationModel = generationModel;
	}

	public Integer getChunksPerQuestion() {
		return chunksPerQuestion;
	}

	public void setChunksPerQuestion(Integer chunksPerQuestion) {
		this.chunksPerQuestion = chunksPerQuestion;
	}

	public Integer getMinChunksPerTest() {
		return minChunksPerTest;
	}

	public void setMinChunksPerTest(Integer minChunksPerTest) {
		this.minChunksPerTest = minChunksPerTest;
	}

	public Integer getQuestionGenerationBatchSize() {
		return questionGenerationBatchSize;
	}

	public void setQuestionGenerationBatchSize(Integer questionGenerationBatchSize) {
		this.questionGenerationBatchSize = questionGenerationBatchSize;
	}

	public Integer getMinMultipleCorrectPercentage() {
		return minMultipleCorrectPercentage;
	}

	public void setMinMultipleCorrectPercentage(Integer minMultipleCorrectPercentage) {
		this.minMultipleCorrectPercentage = minMultipleCorrectPercentage;
	}

	public Integer getMaxConsecutiveZeroProgressBatches() {
		return maxConsecutiveZeroProgressBatches;
	}

	public void setMaxConsecutiveZeroProgressBatches(Integer maxConsecutiveZeroProgressBatches) {
		this.maxConsecutiveZeroProgressBatches = maxConsecutiveZeroProgressBatches;
	}
}
