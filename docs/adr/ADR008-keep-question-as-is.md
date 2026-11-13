# ADR008 - Keep a Flagged Question As It Is
* Date: Nov 13, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

A fourth review action, **Keep as is**, lets the reviewer overrule the verifier:

- `POST /api/tests/{testId}/questions/{questionId}/dismiss-flags` with
  `{expectedCurrentVersionId, comment}` creates a new version with **the same content and no flags**: question
  text, options, answer key, explanation and sources are copied unchanged, and every option loses its flag,
  suggestion and reason.
- The version has origin `FLAGS_DISMISSED`. It keeps the `generationRunId` and `verificationRunId` of the version it
  copies, so the LLM calls that wrote and flagged the content can still be found.
- A `FLAGS_DISMISSED` event records the actor, the comment and, in its payload, every dismissed flag (option text,
  suggestion and reason).
- **The comment is required** (`400` without one): the reviewer is overruling the verifier and must say why.
- It is allowed only while the current version has flags (`409` otherwise), and like every review action only while
  the test is reviewable, through `TestReviewTransactionalServiceImpl.applyNewVersion` (optimistic switch on
  `expectedCurrentVersionId`).
- The question becomes `OK`, and the test `REVIEWED` once no question needs review.

`V2__keep_question_as_is.sql` adds `FLAGS_DISMISSED` to the `chk_tq_versions_origin` and `chk_test_events_type`
CHECK constraints. The UI shows a **Keep as is** button next to Accept fix, with a dialog that asks for the comment.

This extends [ADR001](ADR001-review-process.md), which has three review actions.

## Context

ADR001 left out a "keep as is" action on purpose: accepting the verifier's suggestion was meant to give a valid
question whether or not the verifier was right, because the suggestion replaces a distractor with another clearly
false one. Two things changed:

- **A wrong flag costs a good distractor.** On "What is a disadvantage of setter injection in Spring Boot?" the
  verifier flagged two distractors that the sources list as advantages of constructor injection. Accepting the fix
  replaces good distractors with weaker ones, only to mark the question as reviewed.
- **Accept fix is not always possible.** A suggestion that repeats an existing option (e.g. "@Value" when D is
  already "@Value") is kept but cannot be accepted (`TestQuestionVersion.isFixApplicable`). The only ways out were
  Regenerate, which throws away the question, or Edit, which pre-fills the suggestions and records the result as a
  manual question written by the reviewer.

Versions are immutable and the question status is derived from the current version's flags
(`TestQuestion.deriveStatus`), so clearing flags in place is not possible without breaking ADR001.

## Options Considered

### Option 1 (SELECTED): New version without flags (`FLAGS_DISMISSED`)

#### Consequences
* Adopted because: it follows the ADR001 model. Versions stay immutable, the status stays derived from the current
  version, and the history shows what was flagged, who kept the question and why.
* Adopted because: the reviewer keeps a good question with one click, instead of retyping it in Edit.
* Adopted despite: the new version has the same content as the previous one, so the version list grows.
* Adopted despite: the verifier's judgement is overruled without a second check. The required comment documents the
  decision.

### Option 2: Clear the flags on the current version

#### Consequences
* Rejected because: versions would no longer be immutable, and the history would lose what the verifier flagged.

### Option 3: Mark the question slot as reviewed without a new version

#### Consequences
* Rejected because: it adds a second source of truth for the question status, which every status derivation would
  have to check, and attempts could not tell which flags were dismissed for the version they used.

### Option 4: Use Edit (manual replace) with the original texts

#### Consequences
* Rejected because: the history records a question written by the reviewer (`MANUAL`), not a review decision on the
  verifier's flags, and the edit form pre-fills the suggestions, so keeping the original means retyping it.

## Supporting Material
* [ADR001 – Test Review Process and Question History](ADR001-review-process.md)
* Migration: `knowgauge-service-rest-api/src/main/resources/db/migration/V2__keep_question_as_is.sql`
* Review actions: [03 – Domain model](../architecture/03-domain-model.md#review-actions)
