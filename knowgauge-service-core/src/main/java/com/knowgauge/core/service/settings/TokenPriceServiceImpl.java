package com.knowgauge.core.service.settings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.TokenPrice;
import com.knowgauge.core.model.TokenPriceSuggestion;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.port.repository.TokenPriceRepository;
import com.knowgauge.core.port.repository.UsageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TokenPriceServiceImpl implements TokenPriceService {

	/** Length of the {@code token_prices.model} column. */
	static final int MAX_MODEL_LENGTH = 100;

	private final TokenPriceRepository repository;
	private final TokenPriceCatalog catalog;
	private final UsageRepository usageRepository;
	private final SystemSettingsService systemSettings;

	public TokenPriceServiceImpl(TokenPriceRepository repository, TokenPriceCatalog catalog,
			UsageRepository usageRepository, SystemSettingsService systemSettings) {
		this.repository = repository;
		this.catalog = catalog;
		this.usageRepository = usageRepository;
		this.systemSettings = systemSettings;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TokenPrice> list() {
		return repository.findAll();
	}

	@Override
	@Transactional
	public TokenPrice save(String model, BigDecimal inputUsdPerMillion, BigDecimal outputUsdPerMillion) {
		String name = requireModel(model);
		requirePrice("Input", inputUsdPerMillion);
		requirePrice("Output", outputUsdPerMillion);
		TokenPrice price = repository.findByModel(name).orElseGet(() -> TokenPrice.builder().model(name).build());
		price.setInputUsdPerMillion(inputUsdPerMillion);
		price.setOutputUsdPerMillion(outputUsdPerMillion);
		TokenPrice saved = repository.save(price);
		log.info("Token price of {} set to {} in / {} out USD per million tokens", name, inputUsdPerMillion,
				outputUsdPerMillion);
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> knownModels() {
		Set<String> models = new TreeSet<>();
		// The models in effect on the System settings page (AI models: generation and verification), overrides included.
		systemSettings.list().stream().filter(s -> "AI models".equals(s.definition().group())).map(s -> s.value())
				.filter(m -> m != null && !m.isBlank()).forEach(models::add);
		models.addAll(usageRepository.findUsedModels());
		return List.copyOf(models);
	}

	@Override
	public List<String> catalogModels() {
		return catalog.chatModels("openai");
	}

	@Override
	@Transactional
	public void delete(String model) {
		String name = requireModel(model);
		TokenPrice price = repository.findByModel(name)
				.orElseThrow(() -> new ResourceNotFoundException("No token price for model " + name + "."));
		repository.delete(price);
		log.info("Token price of {} removed", name);
	}

	@Override
	public TokenPriceSuggestion suggest(String model) {
		return catalog.lookUp(requireModel(model));
	}

	private static String requireModel(String model) {
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("The model name is required.");
		}
		String name = model.trim();
		if (name.length() > MAX_MODEL_LENGTH) {
			throw new IllegalArgumentException("The model name may have at most " + MAX_MODEL_LENGTH + " characters.");
		}
		return name;
	}

	private static void requirePrice(String label, BigDecimal price) {
		if (price == null) {
			throw new IllegalArgumentException(label + " price is required.");
		}
		if (price.signum() < 0) {
			throw new IllegalArgumentException(label + " price may not be negative.");
		}
	}
}
