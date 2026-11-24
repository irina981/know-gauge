package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.TokenPrice;
import com.knowgauge.core.port.repository.TokenPriceRepository;
import com.knowgauge.infra.repository.jpa.mapper.TokenPriceEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TokenPriceJpaRepository;

@Repository
public class TokenPriceJpaRepositoryAdapter implements TokenPriceRepository {

	private final TokenPriceJpaRepository jpaRepository;
	private final TokenPriceEntityMapper mapper;

	public TokenPriceJpaRepositoryAdapter(TokenPriceJpaRepository jpaRepository, TokenPriceEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public List<TokenPrice> findAll() {
		return jpaRepository.findAllByOrderByModelAsc().stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<TokenPrice> findByModel(String model) {
		return jpaRepository.findByModel(model).map(mapper::toDomain);
	}

	@Override
	public TokenPrice save(TokenPrice price) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(price)));
	}

	@Override
	public void delete(TokenPrice price) {
		jpaRepository.deleteById(price.getId());
	}
}
