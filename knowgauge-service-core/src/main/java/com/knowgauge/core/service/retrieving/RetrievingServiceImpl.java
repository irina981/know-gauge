package com.knowgauge.core.service.retrieving;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocChunkCount;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.port.embedding.EmbeddingService;
import com.knowgauge.core.port.vectorstore.VectorStore;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RetrievingServiceImpl implements RetrievingService {
	private final VectorStore vectorStore;
	private final EmbeddingService embeddingService;
	private final FocusedRetrievalProperties focused;

	public RetrievingServiceImpl(VectorStore vectorStore, EmbeddingService embeddingService,
			FocusedRetrievalProperties focused) {
		this.vectorStore = vectorStore;
		this.embeddingService = embeddingService;
		this.focused = focused;
	}

	@Override
	public List<ChunkEmbedding> retrieveTop(Collection<Long> tenantIds, Collection<Long> documentIds, int limit,
			TestCoverageMode coverageMode, String focusQuery, boolean avoidRepeats) {
		// =========================
		// Step 0: Validate inputs
		// =========================
		// tenantIds are mandatory for isolation.
		// limit<=0 returns empty.
		// empty documentIds means "no corpus scope" -> return empty (no candidates).
		if (tenantIds == null || tenantIds.isEmpty()) {
			throw new IllegalArgumentException("tenantIds must not be empty");
		}
		if (limit <= 0) {
			return List.of();
		}
		if (documentIds == null || documentIds.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 1: FOCUSED is a similarity search, not balanced sampling
		// =========================
		if (coverageMode == TestCoverageMode.FOCUSED) {
			return retrieveFocused(tenantIds, documentIds, limit, focusQuery);
		}

		// =========================
		// Step 2: Determine embedding model used in the vector store
		// =========================
		// IMPORTANT:
		// - generationModel (LLM) != embeddingModel (vector space model)
		// - embeddings are comparable only within the same embedding_model
		String embeddingModel = embeddingService.modelName();

		// =========================
		// Step 3: Discover corpus shape (chunks per document in scope)
		// =========================
		// We need doc-level chunk counts because "balanced" selection depends on:
		// - how many documents exist in the scope
		// - how large each document is (chunk volume)
		//
		// Example: one 100-page doc + many 3-page docs:
		// - PER_DOC may under-represent the big doc
		// - PER_DOC_CHUNKS keeps proportional representation
		List<DocChunkCount> docCounts = vectorStore.loadDocChunkCounts(tenantIds, embeddingModel, documentIds);
		if (docCounts.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 4: Compute per-document quotas (allocation)
		// =========================
		// For each document: documentId -> maxChunks allowed from that doc.
		//
		// BALANCED_PER_DOCS:
		// - each document gets roughly ceil(limit/docCount)
		//
		// BALANCED_PER_DOC_CHUNKS:
		// - quotas proportional to each document's chunkCount
		// - rounding drift is corrected so total allocated ~= limit
		Map<Long, Integer> maxChunksPerDocumentMap = calculateMaxChunksPerDocument(docCounts, limit, coverageMode);

		// Max quota among all documents; used to over-fetch bounded candidates per doc.
		int maxChunksPerDoc = maxChunksPerDocumentMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		if (maxChunksPerDoc <= 0) {
			return List.of();
		}

		// =========================
		// Step 5: Fetch ranked candidates from DB (bounded per-doc sampling)
		// =========================
		// Over-fetch up to 'maxChunks' candidates per document using:
		// row_number() OVER (PARTITION BY document_id ORDER BY random())
		//
		// This prevents pulling all embeddings for huge documents.
		// Final per-document quotas are applied in memory.
		List<ChunkEmbedding> candidates = vectorStore.findCandidates(tenantIds, documentIds, maxChunksPerDoc, embeddingModel,
				avoidRepeats);

		// =========================
		// Step 6: Final selection in memory (apply quotas + dedupe + limit)
		// =========================
		// - shuffle candidates to mix documents
		// - enforce per-doc quota
		// - dedupe by chunk_checksum (prevents identical content dominating)
		// - stop at requested limit
		//
		// TODO: avoidRepeats
		// - currently avoidRepeats has no effect
		// - should exclude chunks already used in previous tests / sessions
		// - requires an "excludedChunkIds" or retrieval context input
		Map<Long, Integer> usedPerDoc = new HashMap<>();
		Set<String> seenChecksums = new HashSet<>();
		List<ChunkEmbedding> result = new ArrayList<>(limit);

		// Shuffle to mix docs; prevents taking all candidates from the same doc early
		// due to list ordering.
		List<ChunkEmbedding> shuffledCandidates = new ArrayList<>(candidates);
		Collections.shuffle(shuffledCandidates);

		for (ChunkEmbedding e : shuffledCandidates) {

			if (result.size() >= limit) {
				break;
			}

			Long docId = e.getDocumentId();

			// Document budget (max allowed chunks)
			int maxChunksForDoc = maxChunksPerDocumentMap.getOrDefault(docId, 0);
			if (maxChunksForDoc <= 0) {
				continue;
			}

			// How many have we already taken from this document?
			int used = usedPerDoc.getOrDefault(docId, 0);
			if (used >= maxChunksForDoc) {
				continue;
			}

			// Dedupe: identical content should not dominate the selection.
			String checksum = e.getChunkChecksum();
			if (checksum != null && !seenChecksums.add(checksum)) {
				continue;
			}

			usedPerDoc.put(docId, used + 1);
			result.add(e);
		}

		return result;
	}

	/**
	 * FOCUSED coverage: embeds the focus text with the same model as the chunks and
	 * returns only the chunks that are about it, nearest first. A chunk counts when
	 * it is at most {@code maxGapToBest} further away than the closest chunk and no
	 * further than {@code maxDistance}. At most {@code limit} are used, the same
	 * limit as for the balanced modes (it grows with the question count). So a
	 * narrow focus yields few chunks instead of being padded with unrelated ones,
	 * and a focus the material does not cover yields none. There is no
	 * per-document balancing: the focus decides which documents contribute.
	 * Identical content (same checksum) is taken once.
	 */
	private List<ChunkEmbedding> retrieveFocused(Collection<Long> tenantIds, Collection<Long> documentIds, int limit,
			String focusQuery) {
		if (focusQuery == null || focusQuery.isBlank()) {
			throw new IllegalArgumentException("FOCUSED coverage needs a focus text to search for.");
		}
		String embeddingModel = embeddingService.modelName();
		float[] queryEmbedding = embeddingService.embed(focusQuery);
		int wanted = limit;

		// A few extra rows make up for duplicates; the distance cut-off below decides the rest.
		List<ChunkEmbedding> nearest = vectorStore.findNearest(tenantIds, documentIds, queryEmbedding, embeddingModel,
				wanted * 2 + 5);
		if (nearest.isEmpty()) {
			return List.of();
		}
		double best = cosineDistance(queryEmbedding, nearest.get(0).getEmbedding());
		double cutOff = Math.min(best + focused.getMaxGapToBest(), focused.getMaxDistance());

		Set<String> seenChecksums = new HashSet<>();
		List<ChunkEmbedding> result = new ArrayList<>(wanted);
		List<String> distances = new ArrayList<>();
		for (ChunkEmbedding e : nearest) {
			double distance = cosineDistance(queryEmbedding, e.getEmbedding());
			if (result.size() >= wanted || distance > cutOff) {
				break;
			}
			String checksum = e.getChunkChecksum();
			if (checksum != null && !seenChecksums.add(checksum)) {
				continue;
			}
			result.add(e);
			distances.add(e.getChunkId() + "=" + String.format(java.util.Locale.ROOT, "%.3f", distance));
		}
		log.info("Focused retrieval: {} chunks within cosine distance {} (closest {}, at most {}): [{}]",
				result.size(), String.format(java.util.Locale.ROOT, "%.3f", cutOff),
				String.format(java.util.Locale.ROOT, "%.3f", best), wanted, String.join(", ", distances));
		return result;
	}

	/** 1 - cosine similarity, as pgvector's {@code <=>} operator. */
	static double cosineDistance(float[] a, float[] b) {
		if (a == null || b == null || a.length != b.length) {
			return 2.0; // not comparable: treat as far away
		}
		double dot = 0;
		double normA = 0;
		double normB = 0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
		}
		return normA == 0 || normB == 0 ? 2.0 : 1 - dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	/**
	 * Calculates how many chunks EACH document is allowed to contribute to the
	 * final result.
	 *
	 * Output: documentId -> maxChunksFromThatDocument
	 *
	 * Strategies: - BALANCED_PER_DOCS: Each document gets roughly
	 * ceil(limit/docCount). Maximizes diversity across docs.
	 *
	 * - BALANCED_PER_DOC_CHUNKS: Each document quota is proportional to its
	 * available chunk volume. Handles uneven document sizes better.
	 *
	 * Notes: - Some documents may end up with 0 quota after proportional rounding
	 * (small docs). - Rounding drift is corrected so total allocated ~= limit.
	 */
	private Map<Long, Integer> calculateMaxChunksPerDocument(List<DocChunkCount> docCounts, int limit,
			TestCoverageMode mode) {

		Map<Long, Integer> maxChunksPerDocumentMap = new HashMap<>();

		int docN = docCounts.size();

		if (mode == TestCoverageMode.BALANCED_PER_DOCS) {
			int perDoc = (int) Math.ceil((double) limit / docN);
			perDoc = Math.max(1, Math.min(limit, perDoc));

			for (DocChunkCount dc : docCounts) {
				maxChunksPerDocumentMap.put(dc.documentId, perDoc);
			}
			return maxChunksPerDocumentMap;
		}

		// BALANCED_PER_DOC_CHUNKS (proportional to corpus size)
		long totalChunks = docCounts.stream().mapToLong(dc -> dc.chunkCount).sum();
		if (totalChunks <= 0) {
			return maxChunksPerDocumentMap;
		}

		// 1) Initial proportional allocation
		for (DocChunkCount dc : docCounts) {
			double share = (double) dc.chunkCount / (double) totalChunks;
			int q = (int) Math.round(share * limit);
			maxChunksPerDocumentMap.put(dc.documentId, Math.max(0, q));
		}

		// 2) Fix rounding drift (ensure total allocated ~= limit)
		int allocated = maxChunksPerDocumentMap.values().stream().mapToInt(Integer::intValue).sum();
		int diff = limit - allocated;

		// Under-allocation: distribute remaining slots to biggest documents first
		if (diff > 0) {
			List<DocChunkCount> sortedBySizeDesc = docCounts.stream()
					.sorted((a, b) -> Long.compare(b.chunkCount, a.chunkCount)).toList();

			for (DocChunkCount dc : sortedBySizeDesc) {
				if (diff == 0)
					break;
				maxChunksPerDocumentMap.put(dc.documentId, maxChunksPerDocumentMap.get(dc.documentId) + 1);
				diff--;
			}
		}

		// Over-allocation: remove slots from documents with largest current budgets
		// first
		if (diff < 0) {
			int toRemove = -diff;

			List<DocChunkCount> sortedByBudgetDesc = docCounts.stream()
					.sorted((a, b) -> Integer.compare(maxChunksPerDocumentMap.getOrDefault(b.documentId, 0),
							maxChunksPerDocumentMap.getOrDefault(a.documentId, 0)))
					.toList();

			for (DocChunkCount dc : sortedByBudgetDesc) {
				if (toRemove == 0)
					break;

				int cur = maxChunksPerDocumentMap.getOrDefault(dc.documentId, 0);
				if (cur > 0) {
					maxChunksPerDocumentMap.put(dc.documentId, cur - 1);
					toRemove--;
				}
			}
		}

		return maxChunksPerDocumentMap;
	}

}
