# 08 – REST API

The API is served by `knowgauge-service-rest-api`. OpenAPI docs are at `/v3/api-docs` and Swagger UI at
`/swagger-ui.html`. A static landing page (`static/index.html`, with `logo.svg`, `logo-dark.svg` and `favicon.svg`) is served at `/`. Actuator is at
`/actuator` (health with liveness/readiness probes, info, metrics, env, beans, loggers).

There is **no authentication**. The tenant and user come from `ExecutionContext` (hard-wired to `1`).

## Content – `ContentController` (`/api/content`)

| Method | Path | Body / params | Result |
|---|---|---|---|
| GET | `/topics/{id}` | – | `200 TopicDto`, `404` if not found |
| GET | `/topics/roots` | – | `200 List<TopicDto>` |
| GET | `/topics/children/{parentId}` | – | `200 List<TopicDto>` (direct children) |
| GET | `/topics/descendants/{parentId}` | – | `200 List<TopicDto>` (all descendants, by path prefix) |
| POST | `/topics` | `TopicCreateInput` (parentId?, name, description) | `201 TopicDto` + `Location` |
| PUT | `/topics/{id}` | `TopicUpdateInput` (name, description; `null` = unchanged, body `id` ignored) | `200 TopicDto`. `404` if not found, `400` on a duplicate name under the same parent |
| POST | `/topics/tree` | `TopicTreeNodeInput` (recursive name/description/children) | `201 TopicDto` (root with created children) |
| DELETE | `/topics/{id}` | – | `200` (DB cascade deletes subtopics, documents and chunks. MinIO objects and embeddings are **not** cleaned up) |
| GET | `/documents/{id}` | – | `200 DocumentDto` (includes ingestion `status`, `errorMessage`, `ingestedAt`), `404` if not found |
| GET | `/documents/all/{topicId}` | Spring `Pageable` (`page`, `size`, `sort`) | `200 Page<DocumentDto>` |
| POST | `/documents` | `multipart/form-data`: part `meta` = JSON `DocumentInput` (topicId, title), part `file` | `201 DocumentDto` + `Location`. Max 50 MB |
| GET | `/documents/{id}/content` | – | Binary stream with `Content-Type` and `Content-Disposition: attachment`. `404` if not found |
| DELETE | `/documents/{id}` | – | `200` (DB row, then MinIO object) |
| POST | `/documents/ingestion/{id}` | – | `200` once ingestion has finished (synchronous), `409` if the document is already being ingested |

## Tests – `TestGenerationController` (`/api/tests`)

| Method | Path | Body / params | Result |
|---|---|---|---|
| POST | `` | `TestInput` | `201 TestDto` + `Location` (synchronous generation, can take tens of seconds) |
| GET | `/{id}` | – | `200 TestDto` or `404` |
| GET | `/{id}/questions` | – | `200 List<TestQuestionDto>` (current version of each question) sorted by `questionIndex`, or `404` |
| GET | `` | `status` (optional, case-insensitive `TestStatus`) | `200 List<TestDto>` for the tenant |
| DELETE | `/{id}` | – | `200` (cascades to questions, versions, events and generation runs) |

## Review & history – `TestReviewController` (`/api/tests/{testId}`)

See [ADR001](../adr/ADR001-review-process.md). Every action needs the `expectedCurrentVersionId` the reviewer looked
at (`currentVersionId` from the question DTO). It returns the question with its new current version.

| Method | Path | Body / params | Result |
|---|---|---|---|
| POST | `/questions/{questionId}/accept-fix` | `ReviewActionInput {expectedCurrentVersionId, comment?}` | `200 TestQuestionDto`. `409` if the version is stale, the test is not reviewable, or there is no applicable fix |
| POST | `/questions/{questionId}/regenerate` | `ReviewActionInput` | `200 TestQuestionDto` (the new version may itself be `NEEDS_REVIEW`). `409` as above. `502` if the LLM could not produce a valid, non-duplicate question (nothing changed, `REGENERATION_FAILED` recorded) |
| POST | `/questions/{questionId}/manual-replace` | `ManualQuestionInput {expectedCurrentVersionId, questionText, options: {A..D: {text, correct}}, explanation?, comment?}` | `200 TestQuestionDto`. `400` listing every validation reason. `409` as above |
| POST | `/publish` | `PublishTestInput {comment?}` (body optional) | `200 TestDto` with status `PUBLISHED` ([ADR005](../adr/ADR005-publish-tests.md)). `409` if already published, not `GENERATED`/`REVIEWED`, or a question needs review |
| POST | `/unpublish` | `PublishTestInput {comment?}` (body optional) | `200 TestDto` with the status before publishing (`GENERATED` or `REVIEWED`). `409` if the test is not published or attempts exist for it |
| GET | `/questions/{questionId}/versions` | – | `200 List<TestQuestionVersionDto>`, oldest first |
| GET | `/events` | `questionId` (optional) | `200 List<TestEventDto>`, the history trail, oldest first |
| GET | `/generation-runs` | – | `200 List<GenerationRunDto>`: every LLM call with raw response and tokens |

Review actions are allowed while the test is `GENERATED`, `REQUIRES_REVIEW` or `REVIEWED`. A `PUBLISHED` test is
frozen: every review action returns 409 until it is unpublished (possible only while it has no attempts).

### `TestInput`

```json
{
  "topicIds": [2000],
  "documentIds": [1],
  "difficulty": "MEDIUM",
  "avoidRepeats": true,
  "coverageMode": "BALANCED_PER_DOC_CHUNKS",
  "questionCount": 10,
  "answerCardinality": "MULTIPLE_CORRECT",
  "language": "EN",
  "generationModel": "gpt-4.1",
  "promptTemplateId": "mcq-default",
  "generationParams": {
    "temperature": 0.2, "maxOutputTokens": 1200,
    "verificationModel": "gpt-4.1", "verificationTemperature": 0, "verificationMaxOutputTokens": 2000
  }
}
```

Every field is optional and falls back to `kg.testgen.defaults.*`, but at least one topic or document ID is
required. Enum fields are strings, parsed case-insensitively by `EnumParser` through `EnumMappingHelper`.

### `TestQuestionDto`

```json
{
  "id": 9001, "testId": 5001, "questionIndex": 0,
  "status": "NEEDS_REVIEW",
  "currentVersionId": 12001, "versionNo": 1, "origin": "GENERATED",
  "questionText": "…",
  "options": {
    "A": { "letter": "A", "text": "…", "correct": true,  "flagged": false, "suggestedText": null, "flagReason": null },
    "B": { "letter": "B", "text": "…", "correct": false, "flagged": true,  "suggestedText": "…", "flagReason": "…" },
    "C": { … }, "D": { … }
  },
  "correctOptions": ["A"],
  "flaggedOptions": ["B"],
  "explanation": "…",
  "sourceChunkIds": [10101, 10102]
}
```

`correctOptions` is the answer key. `flaggedOptions` lists the distractors the verifier judged possibly true;
each has a suggested replacement (see [03](03-domain-model.md#questions-versions-and-review-adr001)).
`TestQuestionVersionDto` has the same content fields plus `versionNo`, `origin`, `parentVersionId`,
`generationRunId`, `verificationRunId`, `createdAt` and `createdBy`.

## Error model

`GlobalControllerExceptionHandler` (`@RestControllerAdvice`) returns `HttpErrorInfo`
serialized as `{timestamp, path, status, error, message}`:

| Exception | HTTP |
|---|---|
| `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException` (enum errors get a friendlier message) | 400 |
| `IllegalArgumentException` (the domain's "not found", "duplicate" and "invalid state" errors) | 400 |
| `UnsupportedOperationException` | 400 |
| `EntityNotFoundException`, `ResourceNotFoundException` (review: test/question not found) | 404 |
| `ReviewConflictException` (stale version, test not reviewable, no fix to apply, test not publishable, unpublish with attempts) | 409 |
| `QuestionValidationException` (manual replace; message lists every reason) | 400 |
| `RegenerationFailedException` (nothing was changed) | 502 |
| `InsufficientContextException` (test generation: fewer chunks than `questionCount × min-chunks-per-question`) | 422 |
| `DataIntegrityViolationException`, `DuplicateDocumentName/ContentException` | 409 |
| `StorageNotFound` / `StorageAuth` / `StorageBadRequest` / `StorageUnavailable` / `StorageUnexpected` | 404 / 401 / 400 / 503 / 500 |
| `EmbeddingAuth` / `EmbeddingBadRequest` / `EmbeddingRateLimited` / `EmbeddingUnavailable` / `EmbeddingUnexpected` | 401 / 400 / 429 / 503 / 500 |
| `LlmResponseParsingException` `LENGTH` / `PARSING_ERROR`,`MAPPING_ERROR`,`INVALID_RESPONSE` / `OTHER` | 507 / 502 / 500 |
| `HttpClientErrorException`, `RestClientResponseException` | Downstream status |
| anything else | 500 |

Behaviour to know about:

- `IngestionServiceImpl.ingest` and `TestGenerationServiceImpl.generate` wrap every failure that happens after
  their initial checks in a plain `RuntimeException`. Embedding, storage and LLM errors raised during these flows
  therefore come back as **500**, not as the typed statuses above. The typed handlers apply to direct calls such as
  upload and download.
- "Not found" for documents and topics in most core service methods is an `IllegalArgumentException`, so it returns
  **400**, not 404. The single-resource endpoints (`GET`/`PUT /topics/{id}`, `GET /documents/{id}`,
  `GET /documents/{id}/content`) return 404.
- `DuplicateDocument*Exception` classes exist, but `DocumentServiceImpl` throws `IllegalArgumentException` (400).
  The DB unique constraints act as a 409 safety net.

## Contract module

The `knowgauge-service-rest-contract` records are the public API surface: `TopicDto`, `TopicCreateInput`,
`TopicUpdateInput`, `TopicTreeNodeInput`, `DocumentDto`, `DocumentInput`, `DocumentChunkDto`
(no endpoint yet), `TestInput`, `TestDto`, `TestQuestionDto`, `TestQuestionOptionDto`, `TestQuestionVersionDto`,
`TestEventDto`, `GenerationRunDto`, `ReviewActionInput` and `ManualQuestionInput`. They use
`jakarta.validation` and `@Schema` annotations. `knowgauge-service-rest-client` is meant to hold a typed client
over these DTOs but is empty.
