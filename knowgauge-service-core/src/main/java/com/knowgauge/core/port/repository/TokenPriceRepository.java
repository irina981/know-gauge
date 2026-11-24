package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.TokenPrice;

/** The system-wide LLM token prices. */
public interface TokenPriceRepository {

	/** @return every price, by model name */
	List<TokenPrice> findAll();

	Optional<TokenPrice> findByModel(String model);

	TokenPrice save(TokenPrice price);

	void delete(TokenPrice price);
}
