package com.knowgauge.core.service.settings;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.knowgauge.core.model.EmbeddingModelPrice;
import com.knowgauge.core.port.embedding.EmbeddingService;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.port.vectorstore.VectorStore;

@Service
public class EmbeddingSettingsServiceImpl implements EmbeddingSettingsService {

	private final EmbeddingService embeddingService;
	private final TokenPriceCatalog catalog;
	private final VectorStore vectorStore;
	private final int dimension;

	public EmbeddingSettingsServiceImpl(EmbeddingService embeddingService, TokenPriceCatalog catalog,
			VectorStore vectorStore, @Value("${kg.embedding.dimension}") int dimension) {
		this.embeddingService = embeddingService;
		this.catalog = catalog;
		this.vectorStore = vectorStore;
		this.dimension = dimension;
	}

	@Override
	public EmbeddingInfo info() {
		String model = embeddingService.modelName();
		Optional<EmbeddingModelPrice> price = catalog.embeddingModel(model);
		return new EmbeddingInfo(model, dimension, price.map(EmbeddingModelPrice::catalogModel).orElse(null),
				price.map(EmbeddingModelPrice::inputUsdPerMillion).orElse(null),
				price.map(EmbeddingModelPrice::vectorSize).orElse(null), vectorStore.countByEmbeddingModel());
	}
}
