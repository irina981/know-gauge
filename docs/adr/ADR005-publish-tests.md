# ADR005 - Publishing Tests: Frozen Content Before Attempts
* Date: Nov 12, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

A new test status `PUBLISHED` marks a test as ready for distribution.

- `POST /api/tests/{testId}/publish` (optional body `{comment}`) moves a test to `PUBLISHED`. It is allowed only from
  `GENERATED` or `REVIEWED`, when the test has questions and none of them is `NEEDS_REVIEW`. Otherwise it returns 409.
- Publishing records a `TEST_PUBLISHED` event with the acting user and comment, in the same transaction.
- A `PUBLISHED` test is frozen. Accept fix, regenerate and manual replace return 409, so no new question version
  can be created.
- Only `PUBLISHED` tests accept attempts (`TestStatus.acceptsAttempts()`). The attempt service does not exist yet and
  must enforce this when it is added.
- There is no unpublish. To change a published test, create a new test (a future "clone to draft" action).

Publishing and review actions are serialized on the test row: `applyNewVersion` and `publish` both start with
`SELECT … FOR UPDATE` on the test (`TestRepository.findByTenantIdAndIdForUpdate`) and check the status under that
lock. `TestReviewServiceImpl` also checks the status before calling the LLM, so a regeneration on a published test
fails fast.

## Context

Attempts will record answers against a test. Once someone has taken a test, changing its questions makes results
hard to compare, even though attempt answers point at exact question versions (ADR001). ADR001 left open whether
tests should become read-only once attempts exist, and proposed no.

Two ways to forbid changes were considered: an implicit rule ("no review action once an attempt exists") or an
explicit lifecycle step. Attempts have no service yet, so the rule they depend on can be chosen freely now.

A review action that is already running (for example a regeneration waiting on the LLM) could otherwise commit a
new version just after the test was published. The check before the LLM call is not enough on its own.

## Options Considered

### Option 1 (SELECTED): Explicit `PUBLISHED` status

#### Consequences
* Adopted because: publishing is a deliberate, audited action (actor, time, comment), like the review actions.
* Adopted because: the review guard is the existing status check. `PUBLISHED` is simply not reviewable.
* Adopted because: review code does not depend on attempt tables, and clients read "draft" vs "live" from one field.
* Adopted because: attempt creation needs a single status check.
* Adopted despite: a reviewer must publish explicitly before attempts can start.
* Adopted despite: a published test with a mistake cannot be corrected in place. It must be replaced by a new test.

### Option 2: Forbid review actions while an attempt exists

#### Consequences
* Rejected because: the test locks implicitly the moment someone starts an attempt, possibly in the middle of a
  review, with no recorded decision.
* Rejected because: review would query attempt tables, and the check races with attempt creation unless both lock
  the test row.
* Rejected because: attempts could start on tests that still need review.
* Rejected despite: no extra step for the reviewer.

## Advice
* Deleting a published test (`DELETE /api/tests/{id}`) is still allowed. Once attempts exist, block it or add an
  `ARCHIVED` status (closed to new attempts, still readable).
* "Clone to draft" (copy the current versions as version 1 of a new test) is the planned way to fix a published
  test.

## Supporting Material
* [ADR001 – Test Review Process and Question History](ADR001-review-process.md)
* Migration: `knowgauge-service-rest-api/src/main/resources/db/migration/V2__publish_tests.sql`
