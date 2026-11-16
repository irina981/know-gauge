package com.knowgauge.core.service.invitation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Personal test invitations (ADR009).
 */
@Component
@ConfigurationProperties(prefix = "kg.invitations")
public class InvitationProperties {

	/** Base URL of the UI; invitation links are {@code <base>/i/<token>}. */
	private String linkBaseUrl = "http://localhost:5173";

	/** Most emails accepted in one invite request. */
	private int maxPerRequest = 200;

	public String getLinkBaseUrl() {
		return linkBaseUrl;
	}

	public void setLinkBaseUrl(String linkBaseUrl) {
		this.linkBaseUrl = linkBaseUrl;
	}

	public int getMaxPerRequest() {
		return maxPerRequest;
	}

	public void setMaxPerRequest(int maxPerRequest) {
		this.maxPerRequest = maxPerRequest;
	}

	/** Personal link for an invitation token. */
	public String linkFor(String token) {
		return base() + "/i/" + token;
	}

	/** A participant's results page for an attempt token. */
	public String resultsLinkFor(String attemptToken) {
		return base() + "/r/" + attemptToken;
	}

	private String base() {
		return linkBaseUrl.endsWith("/") ? linkBaseUrl.substring(0, linkBaseUrl.length() - 1) : linkBaseUrl;
	}
}
