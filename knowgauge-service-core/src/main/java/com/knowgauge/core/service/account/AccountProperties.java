package com.knowgauge.core.service.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/** Accounts and roles kept by KnowGauge (ADR012). */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kg.account")
public class AccountProperties {

	/**
	 * Emails that get the ADMIN role when they sign in with a verified email (KG_ADMIN_EMAIL). This is how the
	 * first application admin is made; the identity provider knows nothing about roles. Nothing is revoked when an
	 * email is removed here.
	 */
	private List<String> adminEmails = new ArrayList<>();

	boolean isAdminEmail(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		return adminEmails.stream().filter(e -> e != null && !e.isBlank())
				.anyMatch(e -> e.trim().toLowerCase(Locale.ROOT).equals(normalized));
	}
}
