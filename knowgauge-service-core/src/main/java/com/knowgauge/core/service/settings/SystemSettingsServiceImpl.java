package com.knowgauge.core.service.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Setting;
import com.knowgauge.core.model.SettingConfig;
import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.port.repository.SettingConfigRepository;
import com.knowgauge.core.port.repository.SettingRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Keeps the {@code @ConfigurationProperties} beans in line with the {@code SYSTEM} rows of {@code settings}. Once all
 * beans exist (after Flyway, before the web server takes requests) it reads each setting's type and allowed owners
 * from {@code setting_configs} / {@code setting_config_owners} (the database decides them, ADR014; a defined key
 * missing there stops the startup), remembers each setting's properties-file value and applies the saved overrides.
 * Not transactional: each repository call runs in its own short transaction, and a bean changes only after its row
 * was saved.
 */
@Slf4j
@Service
public class SystemSettingsServiceImpl implements SystemSettingsService, SmartInitializingSingleton {

	private final SettingRepository repository;
	private final SettingConfigRepository configRepository;
	private final Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
	/** Each defined setting's type and owners, from the database; filled at startup. */
	private final Map<String, SettingConfig> configs = new HashMap<>();
	/** The properties-file value of each setting, as read before any override. */
	private final Map<String, String> defaults = new HashMap<>();
	private final Map<String, String> overrides = new HashMap<>();

	public SystemSettingsServiceImpl(SettingRepository repository, SettingConfigRepository configRepository,
			List<SettingsContributor> contributors) {
		this.repository = repository;
		this.configRepository = configRepository;
		for (SettingsContributor contributor : contributors) {
			for (SettingDefinition definition : contributor.definitions()) {
				if (definitions.putIfAbsent(definition.key(), definition) != null) {
					throw new IllegalStateException("Setting " + definition.key() + " is defined twice.");
				}
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		loadConfigs();
		definitions.values().forEach(d -> defaults.put(d.key(), d.reader().get()));
		List<String> applied = new ArrayList<>();
		for (Setting row : repository.findSystem()) {
			SettingDefinition definition = definitions.get(row.getKey());
			if (definition == null || !allows(definition, SettingOwner.SYSTEM)) {
				log.warn("System setting {} in the database is unknown or not system-wide; ignored", row.getKey());
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
		log.info("Settings: {} defined, {} system-wide, {} per tenant, {} overridden from the database{}",
				definitions.size(), definitions(SettingOwner.SYSTEM).size(), definitions(SettingOwner.TENANT).size(),
				applied.size(), applied.isEmpty() ? "" : " " + applied);
	}

	/**
	 * Takes every definition's type and owners from the database. A definition without a {@code setting_configs} row
	 * could never be stored ({@code settings} references it), so the startup stops.
	 */
	private void loadConfigs() {
		Map<String, SettingConfig> stored = new HashMap<>();
		configRepository.findAll().forEach(config -> stored.put(config.getKey(), config));
		List<String> missing = new ArrayList<>();
		configs.clear();
		for (SettingDefinition definition : definitions.values()) {
			SettingConfig config = stored.remove(definition.key());
			if (config == null) {
				missing.add(definition.key());
				continue;
			}
			if (config.getOwners() == null || config.getOwners().isEmpty()) {
				log.warn("Setting {} has no owner in setting_config_owners; no one can change it", definition.key());
			}
			configs.put(definition.key(), config);
		}
		if (!missing.isEmpty()) {
			throw new IllegalStateException("Settings without a row in setting_configs (add a Flyway migration): "
					+ String.join(", ", missing));
		}
		if (!stored.isEmpty()) {
			log.warn("setting_configs lists keys without a definition; their stored values are ignored: {}",
					stored.keySet());
		}
	}

	@Override
	public List<SystemSettingValue> list() {
		return definitions(SettingOwner.SYSTEM).stream().map(this::valueOf).toList();
	}

	@Override
	public List<SystemSettingValue> setAll(Map<String, String> values) {
		// Check every value first, so a page with one wrong value saves nothing.
		Map<String, String> valid = new LinkedHashMap<>();
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			SettingDefinition definition = definitions.get(entry.getKey());
			if (definition == null) {
				throw new ResourceNotFoundException("Unknown system setting " + entry.getKey() + ".");
			}
			if (!allows(definition, SettingOwner.SYSTEM)) {
				throw new IllegalArgumentException(entry.getKey() + " cannot have a system-wide value.");
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
		List<Setting> rows = new ArrayList<>();
		valid.forEach((key, value) -> {
			Setting row = repository.findSystem(key).orElseGet(() -> Setting.system(key, null));
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
		repository.deleteSystem();
		for (SettingDefinition definition : definitions.values()) {
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
	public List<SettingDefinition> definitions() {
		return List.copyOf(definitions.values());
	}

	@Override
	public List<SettingDefinition> definitions(SettingOwner owner) {
		return definitions.values().stream().filter(d -> allows(d, owner)).toList();
	}

	@Override
	public SettingConfig config(String key) {
		SettingConfig config = configs.get(key);
		if (config == null) {
			throw new IllegalStateException("Setting " + key + " is not defined, or the settings are not loaded yet.");
		}
		return config;
	}

	private boolean allows(SettingDefinition definition, SettingOwner owner) {
		SettingConfig config = configs.get(definition.key());
		return config != null && config.allows(owner);
	}

	private SystemSettingValue valueOf(SettingDefinition definition) {
		return new SystemSettingValue(definition, config(definition.key()), defaults.get(definition.key()),
				overrides.containsKey(definition.key()), overrides.get(definition.key()), definition.reader().get(),
				definition.suggestionsOrNone());
	}
}
