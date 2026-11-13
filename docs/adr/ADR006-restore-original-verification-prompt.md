# ADR006 - Restore the Original Verification Prompt
* Date: Nov 12, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

The verification prompts go back to the text they had before ADR003 (commit `b34dc6e`, Feb 26, 2026):

- `incorrect-options-verification.txt` (still the default, `kg.testgen.verification.defaults.prompt-template-id`)
  asks the model to decide for each answer given for the question whether it is TRUE or FALSE, and to return only
  the TRUE answers with a clearly false replacement.
- `incorrect-options-verification-only-flagged.txt` is deleted. ADR001 introduced it as a copy of this prompt;
  with the default restored it would be an unused duplicate.
- `verification-output-template.json` echoes `questionText` and `originalAnswer` again, and `verdict` comes before
  `explanation`.

The rest of ADR003 stays: `VerificationPromptBuilder` still sends `sources`, `sourceChunkIds` and `correctAnswers`
with each question, verification runs on OpenAI `gpt-4.1`, and the output caps and timeouts are unchanged. The
restored prompt does not mention those input fields; the model sees them without instructions on how to use them.

This supersedes the prompt and output template parts of [ADR003](ADR003-grounded-reason-first-verification.md).

Update, Nov 13, 2026: `correctAnswers` is no longer sent. On "What is a disadvantage of setter injection in Spring
Boot?" the verifier flagged two distractors that the sources list as advantages of constructor injection. The
verifier input is now only the question, the distractors and the sources.

## Context

Test 3 (two Spring PDFs) asked "What is a disadvantage of using field injection in Spring Boot?". The ADR003
verifier flagged the distractor "It is concise and simple to write" with the reason "confirmed by the sources, which
list this as an advantage of field injection, not a disadvantage" and `verdict: true`, then suggested "It makes the
class harder to test", which is a correct answer to the question.

The ADR003 prompt told the model to combine the question and the answer into one statement and check that statement.
The model built "Field injection is concise and simple to write" without the question's condition and judged that
bare fact. Its negation paragraph lists explicit negations (never, not, except, should not) and did not help with a
question whose condition is a word such as "disadvantage". The earlier prompt asks whether each answer is correct
for the question as given, which handled these questions in earlier use.

## Options Considered

### Option 1 (SELECTED): Restore the pre-ADR003 prompt and output template as they were

#### Consequences
* Adopted because: it judges each answer against the question instead of as a standalone statement.
* Adopted despite: ADR003 measured an only-flagged prompt with sources on gpt-4.1 and it returned an empty result
  without reasoning, missing the Foam distractor of the Corvane fire-extinguisher question. That risk returns.
* Adopted despite: no explanation is written before the verdict, and false verdicts are not returned, so a
  distractor that passes leaves no reason in the recorded run.
* Adopted despite: replies echo the question and answer text again, which makes them longer (slow on CPU-only Ollama).

### Option 2: Keep ADR003 and replace only the "combine into one statement" step

Keep sources, the answer key and explanation first, and ask whether each answer is a correct answer to the question
exactly as asked.

#### Consequences
* Rejected because: the original prompt is the version known to handle these questions; a new variant would need
  its own tuning.
* Rejected despite: it keeps the measured ADR003 benefits (reasons for every verdict, Foam found in every run).

### Option 3: Extend the negation paragraph with words such as "disadvantage" and "drawback"

#### Consequences
* Rejected because: the failure is the model judging the answer without the question's condition, not a missing
  word; a word list only patches individual cases.

## Advice
* Re-run verification on test 3 (field injection) and on the Corvane fire-extinguisher batch to confirm the restored
  prompt flags the right options with `gpt-4.1`.

## Supporting Material
* [ADR001 - Review process](ADR001-review-process.md)
* [ADR003 - Grounded, reason-first distractor verification](ADR003-grounded-reason-first-verification.md)
* [Test generation](../architecture/06-test-generation.md)
