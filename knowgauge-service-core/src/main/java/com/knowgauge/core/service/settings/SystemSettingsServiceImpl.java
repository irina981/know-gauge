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
import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.core.port.repository.SettingConfigRepository;
import com.knowgauge.core.port.repository.SettingRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Keeps the {@code @ConfigurationProperties} beans in line with the {@code SYSTEM} rows of {@code settings}. Once all
 * beans exist (after Flyway, before the web server takes requests) it remembers each setting's properties-file value
 * and applies the saved overrides. First it checks every definition against the possible keys in
 * {@code setting_configs} (seeded by Flyway): a key missing there, or with another type, stops the startup. Not
 * transactional: each repository call runs in its own short transaction, and a bean changes only after its row was
 * saved.
 */
@Slf4j
@Service
public class SystemSettingsServiceImpl implements SystemSettingsService, SmartInitializingSingleton {

	private final SettingRepository repository;
	private final SettingConfigRepository configRepository;
	private final Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
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
					throw new IllegalStateException("System setting " + definition.key() + " is defined twice.");
				}
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		checkConfigs();
		definitions.values().forEach(d -> defaults.put(d.key(), d.reader().get()));
		List<String> applied = new ArrayList<>();
		for (Setting row : repository.findSystem()) {
			SettingDefinition definition = definitions.get(row.getKey());
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

	/**
	 * Every definition needs a {@code setting_configs} row with the same type, or its values could not be stored
	 * ({@code settings.setting_key} references it).
	 */
	private void checkConfigs() {
		Map<String, SettingType> types = new HashMap<>();
		for (SettingConfig config : configRepository.findAll()) {
			types.put(config.getKey(), config.getType());
		}
		List<String> problems = new ArrayList<>();
		for (SettingDefinition definition : definitions.values()) {
			SettingType type = types.remove(definition.key());
			if (type == null) {
				problems.add(definition.key() + " is not in setting_configs");
			} else if (type != definition.settingType()) {
				problems.add(definition.key() + " is " + type + " in setting_configs but " + definition.settingType()
						+ " in its definition");
			}
		}
		if (!problems.isEmpty()) {
			throw new IllegalStateException("System settings do not match setting_configs (add a Flyway migration): "
					+ String.join("; ", problems));
		}
		if (!types.isEmpty()) {
			log.warn("setting_configs lists keys without a definition; their stored values are ignored: {}",
					types.keySet());
		}
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
			SettingDefinition definition = definitions.get(entry.getKey());
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

	private SystemSettingValue valueOf(SettingDefinition definition) {
		return new SystemSettingValue(definition, defaults.get(definition.key()),
				overrides.containsKey(definition.key()), overrides.get(definition.key()), definition.reader().get(),
				suggestions(definition));
	}

	/** The suggestions of a setting; none when they cannot be had (e.g. the price catalog is unreachable). */
	private static List<String> suggestions(SettingDefinition definition) {
		try {
			return definition.suggestions().get();
		} catch (RuntimeException e) {
			log.warn("Suggestions for system setting {} not available: {}", definition.key(), e.getMessage());
			return List.of();
		}
	}
}
