package com.knowgauge.infra.testgeneration.langchain4j.openai.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.service.settings.SystemSettingDefinition;
import com.knowgauge.core.service.settings.SystemSettingsContributor;

/**
 * The verification model on the System settings page, next to the generation model. It is read on every verification
 * call ({@code LlmIncorrectOptionsVerificationServiceImpl#resolveModelName}), so an override applies at once.
 */
@Component
public class VerificationSystemSettings implements SystemSettingsContributor {

	private final OpenAiVerificationModelProperties verification;
	private final TokenPriceCatalog priceCatalog;

	public VerificationSystemSettings(OpenAiVerificationModelProperties verification,
			TokenPriceCatalog priceCatalog) {
		this.verification = verification;
		this.priceCatalog = priceCatalog;
	}

	@Override
	public List<SystemSettingDefinition> definitions() {
		return List.of(new SystemSettingDefinition("kg.testgen.verification.chat-model.openai.model", "AI models",
				"Answer checking model",
				"The OpenAI model that checks the wrong answers of new questions (verification provider openai). The"
						+ " list shows the OpenAI chat models of the price catalog. Give it a token price too.",
				SystemSettingDefinition.Type.TEXT, List.of(), null, null, false, verification::getModel,
				verification::setModel, () -> priceCatalog.chatModels("openai")));
	}
}
