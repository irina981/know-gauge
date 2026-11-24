package com.knowgauge.core.service.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Setting;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.port.repository.SettingRepository;
import com.knowgauge.core.port.repository.TenantRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TenantSettingsServiceImpl implements TenantSettingsService {

	private final SettingRepository repository;
	private final TenantRepository tenantRepository;
	private final SystemSettingsService systemSettings;

	public TenantSettingsServiceImpl(SettingRepository repository, TenantRepository tenantRepository,
			SystemSettingsService systemSettings) {
		this.repository = repository;
		this.tenantRepository = tenantRepository;
		this.systemSettings = systemSettings;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TenantSettingValue> list(Long tenantId) {
		requireTenant(tenantId);
		return valuesOf(tenantId);
	}

	@Override
	@Transactional
	public List<TenantSettingValue> setAll(Long tenantId, Map<String, String> values) {
		requireTenant(tenantId);
		save(tenantId, values);
		return valuesOf(tenantId);
	}

	@Override
	@Transactional
	public List<TenantSettingValue> resetAll(Long tenantId) {
		requireTenant(tenantId);
		repository.deleteByTenantId(tenantId);
		log.info("Settings of tenant {} reset to the system values", tenantId);
		return valuesOf(tenantId);
	}

	@Override
	@Transactional
	public void setUnlimited(Long tenantId) {
		requireTenant(tenantId);
		Map<String, String> noLimits = new LinkedHashMap<>();
		TenantLimits.KEYS.forEach(key -> noLimits.put(key, TenantLimits.NO_LIMIT));
		save(tenantId, noLimits);
	}

	private List<TenantSettingValue> valuesOf(Long tenantId) {
		Map<String, String> own = ownValues(repository.findByTenantId(tenantId));
		return tenantDefinitions().values().stream().map(d -> {
			String system = d.reader().get();
			String mine = own.get(d.key());
			return new TenantSettingValue(d, system, mine, mine != null ? mine : system);
		}).toList();
	}

	/** Checks all values, then saves the tenant's own values and removes those set empty. */
	private void save(Long tenantId, Map<String, String> values) {
		Map<String, SettingDefinition> definitions = tenantDefinitions();
		// Check every value first, so a dialog with one wrong value saves nothing.
		Map<String, String> valid = new LinkedHashMap<>();
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			SettingDefinition definition = definitions.get(entry.getKey());
			if (definition == null) {
				if (systemSettings.definitions().stream().anyMatch(d -> d.key().equals(entry.getKey()))) {
					throw new IllegalArgumentException(entry.getKey() + " applies to the whole system only.");
				}
				throw new ResourceNotFoundException("Unknown setting " + entry.getKey() + ".");
			}
			String text = entry.getValue() == null ? "" : entry.getValue().trim();
			if (text.isEmpty()) {
				valid.put(entry.getKey(), null); // back to the system value
				continue;
			}
			try {
				valid.put(entry.getKey(), definition.validate(text));
			} catch (IllegalArgumentException e) {
				problems.add(e.getMessage());
			}
		}
		if (!problems.isEmpty()) {
			throw new IllegalArgumentException(String.join(" ", problems));
		}

		Map<String, Setting> rows = repository.findByTenantId(tenantId).stream()
				.collect(Collectors.toMap(Setting::getKey, Function.identity()));
		List<Setting> save = new ArrayList<>();
		List<Setting> remove = new ArrayList<>();
		valid.forEach((key, value) -> {
			Setting row = rows.get(key);
			if (value == null) {
				if (row != null) {
					remove.add(row);
				}
			} else if (row == null) {
				save.add(Setting.tenant(tenantId, key, value));
			} else {
				row.setValue(value);
				save.add(row);
			}
		});
		repository.deleteAll(remove);
		repository.saveAll(save);
		log.info("Settings of tenant {} saved: {}", tenantId, valid);
	}

	@Override
	@Transactional(readOnly = true)
	public TenantLimits limits(Long tenantId) {
		return limitsOf(ownValues(repository.findByTenantId(tenantId)));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, TenantLimits> limits(Collection<Long> tenantIds) {
		Map<Long, List<Setting>> byTenant = repository.findByTenantIdIn(tenantIds).stream()
				.collect(Collectors.groupingBy(Setting::getTenantId));
		Map<Long, TenantLimits> limits = new HashMap<>();
		for (Long tenantId : tenantIds) {
			limits.put(tenantId, limitsOf(ownValues(byTenant.getOrDefault(tenantId, List.of()))));
		}
		return limits;
	}

	@Override
	@Transactional(readOnly = true)
	public Set<Long> withOwnValues(Collection<Long> tenantIds) {
		Set<String> keys = tenantDefinitions().keySet();
		return repository.findByTenantIdIn(tenantIds).stream().filter(s -> keys.contains(s.getKey()))
				.map(Setting::getTenantId).collect(Collectors.toSet());
	}

	/** The tenant's own value of each limit, else the system value. */
	private TenantLimits limitsOf(Map<String, String> own) {
		Map<String, SettingDefinition> definitions = tenantDefinitions();
		return TenantLimits.of(key -> {
			String mine = own.get(key);
			if (mine != null) {
				return mine;
			}
			SettingDefinition definition = definitions.get(key);
			return definition == null ? null : definition.reader().get();
		});
	}

	/** The rows' values by key, leaving out keys that are not (or no longer) tenant-level. */
	private Map<String, String> ownValues(List<Setting> rows) {
		Map<String, SettingDefinition> definitions = tenantDefinitions();
		Map<String, String> values = new HashMap<>();
		for (Setting row : rows) {
			SettingDefinition definition = definitions.get(row.getKey());
			if (definition == null) {
				log.warn("Setting {} of tenant {} is not a tenant setting; ignored", row.getKey(), row.getTenantId());
				continue;
			}
			try {
				values.put(row.getKey(), definition.validate(row.getValue()));
			} catch (IllegalArgumentException e) {
				log.warn("Setting {} of tenant {} has an invalid value '{}' ({}); the system value applies",
						row.getKey(), row.getTenantId(), row.getValue(), e.getMessage());
			}
		}
		return values;
	}

	private Map<String, SettingDefinition> tenantDefinitions() {
		Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
		systemSettings.definitions().stream().filter(SettingDefinition::tenantLevel)
				.forEach(d -> definitions.put(d.key(), d));
		return definitions;
	}

	private void requireTenant(Long tenantId) {
		if (tenantId == null || tenantRepository.findById(tenantId).isEmpty()) {
			throw new ResourceNotFoundException("Tenant " + tenantId + " not found.");
		}
	}
}
