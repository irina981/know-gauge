package com.knowgauge.restapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.model.enums.Role;

/** The {@code @RolesAllowed} names stay equal to the {@link Role} constants (ADR017). */
class RolesTest {

	@Test
	void everyRoleHasAConstantOfTheSameName() {
		String[] constants = Arrays.stream(Roles.class.getDeclaredFields())
				.filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class).map(f -> {
					try {
						assertThat(f.get(null)).isEqualTo(f.getName());
						return f.getName();
					} catch (IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				}).toArray(String[]::new);

		assertThat(constants).containsExactlyInAnyOrder(Arrays.stream(Role.values()).map(Role::name)
				.toArray(String[]::new));
	}
}
