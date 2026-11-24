package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TokenPriceEntity;

@Repository
public interface TokenPriceJpaRepository extends JpaRepository<TokenPriceEntity, Long> {

	List<TokenPriceEntity> findAllByOrderByModelAsc();

	Optional<TokenPriceEntity> findByModel(String model);
}
