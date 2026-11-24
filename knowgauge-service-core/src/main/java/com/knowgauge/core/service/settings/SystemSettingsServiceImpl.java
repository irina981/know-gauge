package com.knowgauge.core.service.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.SystemSetting;
import com.knowgauge.core.port.repository.SystemSettingRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Keeps the {@code @ConfigurationProperties} beans in line with the overrides in {@code system_settings}. Once all
 * beans exist (after Flyway, before the web server takes requests) it remembers each setting's properties-file value
 * and applies the saved overrides. Not transactional: each repository call runs in its own short transaction, and a
 * bean changes only after its row was saved.
 */
@Slf4j
@Service
public class SystemSettingsServiceImpl implements SystemSettingsService, SmartInitializingSingleton {

	private final SystemSettingRepository repository;
	private final TenantLimitProperties tenantLimits;
	private final Map<String, SystemSettingDefinition> definitions = new LinkedHashMap<>();
	/** The properties-file value of each setting, as read before any override. */
	private final Map<String, String> defaults = new HashMap<>();
	private final Map<String, String> overrides = new HashMap<>();

	public SystemSettingsServiceImpl(SystemSettingRepository repository, List<SystemSettingsContributor> contributors,
			TenantLimitProperties tenantLimits) {
		this.repository = repository;
		this.tenantLimits = tenantLimits;
		for (SystemSettingsContributor contributor : contributors) {
			for (SystemSettingDefinition definition : contributor.definitions()) {
				if (definitions.putIfAbsent(definition.key(), definition) != null) {
					throw new IllegalStateException("System setting " + definition.key() + " is defined twice.");
				}
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		definitions.values().forEach(d -> defaults.put(d.key(), d.reader().get()));
		List<String> applied = new ArrayList<>();
		for (SystemSetting row : repository.findAll()) {
			SystemSettingDefinition definition = definitions.get(row.getKey());
			if (definition == null) {
				log.warn("System setting {} in the database is unknown; ignored", row.getKey());
				continue;
			}
			try {
				String value = definition.validate(row.getValue());
				definition.writer().accept(value);
				overrides.put(definition.key(), value);
				applied.add(definition.key());
			} catch (RuntimeException e) {
				log.warn("System setting {} in the database has an invalid value '{}' ({}); the properties value {}"
						+ " applies", row.getKey(), row.getValue(), e.getMessage(), defaults.get(row.getKey()));
			}
		}
		log.info("System settings: {} defined, {} overridden from the database{}", definitions.size(), applied.size(),
				applied.isEmpty() ? "" : " " + applied);
	}

	@Override
	public List<SystemSettingValue> list() {
		return definitions.values().stream().map(this::valueOf).toList();
	}

	@Override
	public List<SystemSettingValue> setAll(Map<String, String> values) {
		// Check every value first, so a page with one wrong value saves nothing.
		Map<String, String> valid = new LinkedHashMap<>();
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			SystemSettingDefinition definition = definitions.get(entry.getKey());
			if (definition == null) {
				throw new ResourceNotFoundException("Unknown system setting " + entry.getKey() + ".");
			}
			try {
				valid.put(entry.getKey(), definition.validate(entry.getValue() == null ? "" : entry.getValue()));
			} catch (IllegalArgumentException e) {
				problems.add(e.getMessage());
			}
		}
		if (!problems.isEmpty()) {
			throw new IllegalArgumentException(String.join(" ", problems));
		}

		// One transaction for all rows (the repository's saveAll), then the beans.
		List<SystemSetting> rows = new ArrayList<>();
		valid.forEach((key, value) -> {
			SystemSetting row = repository.findByKey(key).orElseGet(() -> SystemSetting.builder().key(key).build());
			// An optional setting saved empty is stored as "" (e.g. no limit over a configured limit).
			row.setValue(value == null ? "" : value);
			rows.add(row);
		});
		repository.saveAll(rows);
		valid.forEach((key, value) -> {
			definitions.get(key).writer().accept(value);
			overrides.put(key, value);
		});
		log.info("System settings saved: {}", valid);
		return list();
	}

	@Override
	public List<SystemSettingValue> resetAll() {
		repository.deleteAll();
		for (SystemSettingDefinition definition : definitions.values()) {
			String value = defaults.get(definition.key());
			if (value != null || definition.optional()) {
				definition.writer().accept(value);
			}
		}
		log.info("System settings reset to the properties values; overrides removed: {}", overrides.keySet());
		overrides.clear();
		return list();
	}

	@Override
	public Optional<Long> monthlyTokenLimit() {
		return Optional.ofNullable(tenantLimits.getMonthlyTokenLimit());
	}

	@Override
	public Optional<Long> storageLimitBytes() {
		return Optional.ofNullable(tenantLimits.getStorageLimitMb()).map(mb -> mb * 1024 * 1024);
	}

	private SystemSettingValue valueOf(SystemSettingDefinition definition) {
		return new SystemSettingValue(definition, defaults.get(definition.key()),
				overrides.containsKey(definition.key()), overrides.get(definition.key()), definition.reader().get(),
				suggestions(definition));
	}

	/** The suggestions of a setting; none when they cannot be had (e.g. the price catalog is unreachable). */
	private static List<String> suggestions(SystemSettingDefinition definition) {
		try {
			return definition.suggestions().get();
		} catch (RuntimeException e) {
			log.warn("Suggestions for system setting {} not available: {}", definition.key(), e.getMessage());
			return List.of();
		}
	}
}
