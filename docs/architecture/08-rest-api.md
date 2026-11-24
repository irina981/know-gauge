# 08 – REST API

The API is served by `knowgauge-service-rest-api`. OpenAPI docs are at `/v3/api-docs` and Swagger UI at
`/swagger-ui.html`. A static landing page (`static/index.html`, with `logo.svg`, `logo-dark.svg` and `favicon.svg`) is served at `/`. Actuator is at
`/actuator` (health with liveness/readiness probes, info, metrics, env, beans, loggers).

Who calls the API depends on the security mode (`kg.security.mode`, ADR010):

- `fixed` (default): no authentication. Every request runs as `kg.context.tenant-id` / `user-id`.
- `oidc`: Keycloak access tokens (`Authorization: Bearer …`), checked for issuer, signature and audience
  `knowgauge-api`. The token's subject is mapped to a local user and tenant (created on the first call).
  - Open without a token: `/api/public/**`, `/actuator/health`, `/actuator/info`, the landing page and Swagger.
  - Needs the `ADMIN` role (from `user_roles`, ADR012): `/api/admin/**` and the other actuator endpoints. The token's
    own roles are ignored.
  - Needs the `TENANT_ADMIN` role: `/api/tenant/**` (ADR016).
  - Everything else needs a signed-in user and works in that user's tenant (plus the read-only `LIBRARY` tenant for
    content).
  - Responses: `401` without a valid token, `403` without the role.

## Admin – `AdminController` (`/api/admin`, `ADMIN` role)

| Method | Path | Body / params | Result |
|---|---|---|---|
| GET | `/tenants` | – | `200 TenantSummaryDto[] {tenantId, name, type, ownerUserId, ownerName, ownerEmail, createdAt, topics, documents}`: every tenant (user libraries and the shared library), by name. `403` without `ADMIN` |
| POST | `/tenants` | `TenantCreateInput {type, name, emails[], message}` | `201 TenantCreatedDto {tenant, invitations[]}`: a new workspace (ADR015). `PERSONAL` needs exactly one address, `ORGANIZATION` at least one, `LIBRARY` none (no limits, readable by everyone). Invitees become `TENANT_ADMIN` on accepting; a failed send is kept on the invitation. `400` wrong number of invitees, invalid address or missing name |
| GET | `/tenants/{id}/invitations` | – | `200 TenantInvitationDto[] {id, tenantId, email, role, link, createdAt, sentAt, sendCount, sendError, acceptedAt}`, oldest first |
| POST | `/tenants/{id}/invitations` | `TenantInviteInput {emails[], message, role}` | `200 TenantInvitationDto[]`: any number to an organization, one to a personal workspace without an owner and without an invitation to someone else, none to a library. `role` is `TENANT_ADMIN` (default), `TENANT_USER` or `TENANT_VIEWER`; a personal workspace takes only `TENANT_ADMIN` (ADR016). An address invited before gets its link again, with the new role. `400` other role, `409` personal workspace already taken |
| POST | `/tenants/{id}/invitations/{invitationId}/resend` | – | `200 TenantInvitationDto`, same link. `409` already accepted |
| DELETE | `/tenants/{id}/invitations/{invitationId}` | – | `204`: the link stops working. `409` already accepted |

An `ADMIN` reads any user's workspace through the normal APIs (ADR013): send the tenant id in the `X-KG-Tenant`
header on `GET` requests under `/api/` (not `/api/me`, `/api/admin/**` or `/api/public/**`). The request then works
in that tenant (content, tests, attempts, invitations), content is reported `readOnly`, and any other method with the
header gets `403`. The header is ignored elsewhere, for the admin's own tenant and for a `LIBRARY` tenant; non-admins
get `403`, unknown tenants `404`.

## Workspace – `TenantController` (`/api/tenant`, `TENANT_ADMIN` role)

The signed-in user's own **organization** workspace (ADR016). Every endpoint returns `403` for a workspace that is
not an organization; changes also `403` while an `ADMIN` views another workspace (ADR013).

| Method | Path | Body / params | Result |
|---|---|---|---|
| GET | `/users` | – | `200 TenantUserDto[] {id, email, displayName, roles, createdAt, lastSeenAt}`, by name |
| GET | `/invitations` | – | `200 TenantInvitationDto[]`, oldest first |
| POST | `/invitations` | `TenantInviteInput {emails[], message, role}` | `200 TenantInvitationDto[]`: `role` `TENANT_ADMIN` (default), `TENANT_USER` or `TENANT_VIEWER`. An address invited before gets its link again with the new role; one that joined is returned unchanged. `400` invalid address or role |
| POST | `/invitations/{invitationId}/resend` | – | `200 TenantInvitationDto`, same link. `404` not this workspace's, `409` already accepted |
| DELETE | `/invitations/{invitationId}` | – | `204`: the link stops working. `404` not this workspace's, `409` already accepted |
| GET | `/settings` | – | `200 TenantSettingDto[]`: the workspace's **configuration** (`CONFIG` settings with a `TENANT` owner, ADR014), never its limits |
| PUT | `/settings` | `TenantSettingsInput {values}` | `200 TenantSettingDto[]`: saves the workspace's own values; empty removes one. `400` a value does not fit, `403` a limit |
| DELETE | `/settings` | – | `200 TenantSettingDto[]`: removes the workspace's own configuration values; its limits stay |

## Account – `AccountController` (`/api`)

| Method | Path | Body / params | Result |
|---|---|---|---|
| GET | `/me` | – | `200 CurrentUserDto {userId, tenantId, tenantName, tenantType, email, displayName, roles, securityMode, createdAt}`. The first call with a new user's token creates the user and their personal tenant, or joins the workspace a pending invitation to their verified email is for (ADR015). `roles` lists the user's roles from `user_roles` (ADR012), e.g. `ADMIN` and `TENANT_ADMIN`. In mode `fixed`: the fixed tenant and user, roles `[TENANT_ADMIN]`, no `createdAt` |
| POST | `/me/tenant-invitations/{token}/accept` | – | `200 TenantJoinedDto {tenantId, tenantName, previousTenantId}`: moves the user into the invited workspace (ADR015). `403` another or an unverified email, `404` unknown or revoked link, `409` accepted by someone else or the personal workspace has another owner. Accepting again is a no-op |
| GET | `/me/preferences` | – | `200 UserPreferencesDto {theme}` (`LIGHT`/`DARK`/`SYSTEM`; `null` = never chosen, the app default). Stored as JSON in `users.preferences_json`. All `null` when there is no user row (mode `fixed`) |
| PUT | `/me/preferences` | `UserPreferencesDto` | `200 UserPreferencesDto`. Replaces all settings. `400` unknown value, `404` no user row (mode `fixed`) |
| GET | `/me/usage` | – | `200 UsageSummaryDto`: the own tenant's topics, documents (and ingested), storage bytes, plus this calendar month (UTC): tests created, AI-written question versions (`GENERATED` + `REGENERATED`), submitted attempts and LLM input/output tokens from `generation_runs`. The shared library is not counted; embedding tokens are not tracked yet |

## Content – `ContentController` (`/api/content`)

| Method | Path | Body / params | Result |
|---|---|---|---|
| GET | `/topics/{id}` | – | `200 TopicDto`, `404` if not found |
| GET | `/topics/roots` | – | `200 List<TopicDto>` |
| GET | `/topics/children/{parentId}` | – | `200 List<TopicDto>` (direct children) |
| GET | `/topics/descendants/{parentId}` | – | `200 List<TopicDto>` (all descendants, by path prefix) |
| GET | `/topics/ingested-document-counts` | – | `200 List<TopicDocumentCountDto {topicId, ingestedDocuments}>`: ingested documents directly in each topic; topics without any are left out. Clients add up a subtree to know whether a topic can be used for generation |
| POST | `/topics` | `TopicCreateInput` (parentId?, name, description, libraryTenantId?) | `201 TopicDto` + `Location`. A root topic goes into the own tenant, or for an `ADMIN` into the library `libraryTenantId` (ADR015; `403` for others, `400` if it is not a library) |
| PUT | `/topics/{id}` | `TopicUpdateInput` (name, description; `null` = unchanged, body `id` ignored) | `200 TopicDto`. `404` if not found, `400` on a duplicate name under the same parent |
| POST | `/topics/tree` | `TopicTreeNodeInput` (recursive name/description/children; libraryTenantId? on the root) | `201 TopicDto` (root with created children) |
| DELETE | `/topics/{id}` | – | `200` (DB cascade deletes subtopics, documents and chunks. MinIO objects and embeddings are **not** cleaned up) |
| GET | `/documents/{id}` | – | `200 DocumentDto` (includes ingestion `status`, `errorMessage`, `ingestedAt`), `404` if not found |
| GET | `/documents/all/{topicId}` | Spring `Pageable` (`page`, `size`, `sort`; default sort `id`, i.e. upload order) | `200 Page<DocumentDto>` |
| POST | `/documents` | `multipart/form-data`: part `meta` = JSON `DocumentInput` (topicId, title), part `file` | `201 DocumentDto` + `Location`. Max 50 MB |
| GET | `/documents/{id}/content` | `inline` (default `false`) | Binary stream with `Content-Type` and `Content-Disposition: attachment` (`inline` with `inline=true`, for the UI's PDF preview). `404` if not found |
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
| POST | `/questions/{questionId}/dismiss-flags` | `DismissFlagsInput {expectedCurrentVersionId, comment}` (comment required) | `200 TestQuestionDto` with a new version: same content, no flags ("Keep as is", ADR008). `400` without a comment. `409` if the version is stale, the test is not reviewable, or nothing is flagged |
| POST | `/questions/{questionId}/accept-fix` | `ReviewActionInput {expectedCurrentVersionId, comment?}` | `200 TestQuestionDto`. `409` if the version is stale, the test is not reviewable, or there is no applicable fix |
| POST | `/questions/{questionId}/regenerate` | `ReviewActionInput` | `200 TestQuestionDto` (the new version may itself be `NEEDS_REVIEW`). `409` as above. `502` if the LLM could not produce a valid, non-duplicate question (nothing changed, `REGENERATION_FAILED` recorded) |
| POST | `/questions/{questionId}/manual-replace` | `ManualQuestionInput {expectedCurrentVersionId, questionText, options: {A..D: {text, correct}}, explanation?, comment?}` | `200 TestQuestionDto`. `400` listing every validation reason. `409` as above |
| PUT | `/details` | `TestDetailsInput {name, description?}` | `200 TestDto`. Changes the name (required, max 200) and description (max 2000, blank removes it) in any status except `PUBLISHED` (`409`); records `TEST_DETAILS_UPDATED` |
| POST | `/publish` | `PublishTestInput {comment?}` (body optional) | `200 TestDto` with status `PUBLISHED` ([ADR005](../adr/ADR005-publish-tests.md)). `409` if already published, not `GENERATED`/`REVIEWED`, or a question needs review |
| POST | `/unpublish` | `PublishTestInput {comment?}` (body optional) | `200 TestDto` with the status before publishing (`GENERATED` or `REVIEWED`). `409` if the test is not published or attempts exist for it |
| GET | `/has-attempts` | – | `200 true` once any attempt exists for the test, otherwise `200 false`. Lets clients enable unpublish; `/unpublish` checks again under the test row lock |
| GET | `/questions/{questionId}/versions` | – | `200 List<TestQuestionVersionDto>`, oldest first |
| GET | `/events` | `questionId` (optional) | `200 List<TestEventDto>`, the history trail, oldest first |
| GET | `/generation-runs` | – | `200 List<GenerationRunDto>`: every LLM call with prompt, request params, raw response and tokens |

Review actions are allowed while the test is `GENERATED`, `REQUIRES_REVIEW` or `REVIEWED`. A `PUBLISHED` test is
frozen: every review action returns 409 until it is unpublished (possible only while it has no attempts).

## Attempts – `AttemptController` (`/api`)

The administrator's view of attempts (ADR009). Only `PUBLISHED` tests can be taken. `POST /tests/{testId}/attempts`
starts an attempt for the current user (the signed-in user, or `kg.context.user-id` without sign-in); participants start theirs through
`/api/public/**` below. `AttemptDto` carries `participantName`, `participantEmail` and `invitationId`.

| Method | Path | Body / params | Result |
|---|---|---|---|
| POST | `/tests/{testId}/attempts` | – | `201 AttemptDetailDto` with the questions and options, **without** the answer key. `404` unknown test, `409` test not published or without questions. Locks the test row, so it cannot race with unpublish |
| GET | `/attempts/{attemptId}` | – | `200 AttemptDetailDto`. While `STARTED` the answer key is hidden; once `SCORED` each question has `chosenOptions`, `correctOptions`, `correct` and `explanation` |
| POST | `/attempts/{attemptId}/submit` | `SubmitAttemptInput {answers: [{questionId, chosenOptions}]}` | `200 AttemptDetailDto`, scored. `400` unknown question, invalid letter or several options on a single-answer test. `409` already submitted |
| GET | `/attempts` | `testId`, `userId`, `participantEmail` (all optional; email case-insensitive) | `200 List<AttemptDto>`, newest first |

Scoring is all-or-nothing per question: correct only if the chosen options exactly equal the answer key (for
`MULTIPLE_CORRECT`, every correct option and no other). Questions left out of the submission count as wrong.
`scorePercent` is `correct / total × 100`, rounded to one decimal. Each answer stores the question version it was
given for; published tests are frozen, so that is the current version.

## Workspace invitations – `TenantInvitationController` (`/api/public`)

| Method | Path | Body / params | Result |
|---|---|---|---|
| GET | `/tenant-invitations/{token}` | – | `200 TenantInvitationPreviewDto {tenantName, tenantType, email, accepted}`, no ids, for the join page `/join/{token}` before signing in. `404` unknown or revoked link |

## Public tests – `PublicTestController` (`/api/public`)

What participants call ([ADR009](../adr/ADR009-public-test-links.md)). Everything is addressed by a random token,
never by id, and participants never get the answer key: after scoring only the score and their own answers
marked right or wrong. The score and a results link (`/r/{attemptToken}`) are also emailed after the submit commits. A link works while its test is
`PUBLISHED`; otherwise it answers `404`.

| Method | Path | Body | Result |
|---|---|---|---|
| GET | `/tests/{testToken}` | – | `200 PublicTestDto {name, description, questionCount, answerCardinality}` |
| POST | `/tests/{testToken}/attempts` | `StartPublicAttemptInput {participantName, participantEmail}` | `201 PublicAttemptDto` with the attempt token and the questions (no key). No attempt limit for one person. `400` missing name or invalid email; `409` the test is full (workspace limit "Participants per test": invited addresses and attempts' addresses, each once) and the email is not among its participants yet |
| GET | `/invitations/{invitationToken}` | – | `200 PublicInvitationDto {email, test, attemptStatus?, attemptToken?}`; `attemptToken` only while the attempt runs |
| POST | `/invitations/{invitationToken}/attempt` | `StartInvitationAttemptInput {participantName}` | `200 PublicAttemptDto`. The email is the invited address. Returns the running attempt if already started; `409` once it was submitted |
| GET | `/attempts/{attemptToken}` | – | `200 PublicAttemptDto`: the questions while `STARTED`; once `SCORED`, `correctCount`, `scorePercent` and each question with `chosenOptions` and `correct` (`correctOptions` and `explanation` stay empty) |
| POST | `/attempts/{attemptToken}/submit` | `SubmitAttemptInput` | `200 PublicAttemptDto` with the score and the marked answers; the result email goes out after commit. `409` already submitted |

## Shared link – `SharedLinkController`

| Method | Path | Body | Result |
|---|---|---|---|
| POST | `/api/tests/{testId}/shared-link` | – | `200 TestDto` with a new `publicToken`. Creates the link, or replaces it so the old one stops working (attempts already started can still be finished). Records `SHARED_LINK_CREATED` / `SHARED_LINK_REGENERATED`. `404` unknown test, `409` not published |

A test has no shared link (`publicToken` is `null`) until one is created here.

## Invitations – `InvitationController` (`/api/tests/{testId}/invitations`)

Personal links by email (ADR009): one per address, usable for one attempt, sent by the server over SMTP.

| Method | Path | Body | Result |
|---|---|---|---|
| POST | `` | `InviteInput {emails, message?}` | `200 List<TestInvitationDto>` for the given addresses. Duplicates are merged; already invited addresses get the same link again unless it was used. A failed send is returned in `sendError`, not as an error. `400` invalid or no addresses, or more than `kg.invitations.max-per-request`; `409` test not published, or the new addresses would take the test over its participant limit (the message says how many more fit) |
| GET | `` | – | `200 List<TestInvitationDto>`, oldest first, with `attemptStatus`, `participantName` and `scorePercent` of used links |
| POST | `/{invitationId}/resend` | – | `200 TestInvitationDto`. `409` link already used or test not published |


### `TestInput`

```json
{
  "name": "Java Concurrency – Medium (10 questions)",
  "description": "Practice test for the threads and executors chapters",
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

`name` is required (non-blank after trimming, at most 200 characters). The backend does not invent one: the client
proposes a meaningful default (for example from the selected topics, difficulty and question count) and lets the
user edit it. `description` is optional (at most 2000 characters; blank is stored as `null`). Every other field is
optional and falls back to `kg.testgen.defaults.*`, but at least one topic or document ID is required.
A topic in `topicIds` stands for its whole subtree (its ingested documents and those of all sub-topics).
`coverageMode: "FOCUSED"` additionally requires `focusQuery` (at most 2000 characters): the test is generated from
the chunks most similar to it ([05](05-retrieval.md#focused-similarity-search)). It is ignored for the other modes,
and `TestDto.focusQuery` returns it. Enum fields are strings, parsed case-insensitively by `EnumParser` through `EnumMappingHelper`.

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
| `AttemptConflictException` (test not published or without questions, attempt already submitted) | 409 |
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
`TestEventDto`, `GenerationRunDto`, `ReviewActionInput`, `ManualQuestionInput`, `AttemptDto`, `AttemptDetailDto`,
`AttemptQuestionDto`, `SubmitAttemptInput`, `PublicTestDto`, `PublicAttemptDto`, `PublicInvitationDto`,
`StartPublicAttemptInput`, `StartInvitationAttemptInput`, `InviteInput` and `TestInvitationDto`. They use
`jakarta.validation` and `@Schema` annotations. `knowgauge-service-rest-client` is meant to hold a typed client
over these DTOs but is empty.
