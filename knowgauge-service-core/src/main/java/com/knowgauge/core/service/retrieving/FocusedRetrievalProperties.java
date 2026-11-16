package com.knowgauge.core.service.retrieving;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Which chunks count as "about the focus" for FOCUSED coverage. Distances are
 * cosine distances (0 = same direction); the defaults are tuned for
 * {@code text-embedding-3-small}, where chunks about the focus are usually
 * within 0.2 of the closest one and unrelated text is 0.6 or more away.
 */
@Component
@ConfigurationProperties(prefix = "kg.retrieval.focused")
public class FocusedRetrievalProperties {

	/** A chunk further than this from the focus text is never used. */
	private double maxDistance = 0.65;

	/** A chunk is used only if it is at most this much further away than the closest chunk. */
	private double maxGapToBest = 0.20;

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public double getMaxGapToBest() {
		return maxGapToBest;
	}

	public void setMaxGapToBest(double maxGapToBest) {
		this.maxGapToBest = maxGapToBest;
	}
}
