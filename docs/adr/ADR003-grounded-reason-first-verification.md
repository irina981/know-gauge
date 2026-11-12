# ADR003 - Grounded, Reason-First Distractor Verification
* Date: Nov 12, 2026
* Status: [ACCEPTED] (the prompt and output template were reverted to their earlier text by
  [ADR006](ADR006-restore-original-verification-prompt.md); the sources and answer key in the input, the OpenAI
  default and the output caps stay)
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

The distractor verifier now sees what the generator saw and must reason before it decides:

- **Sources in the prompt.** `VerificationPromptBuilder` sends each chunk the questions cite, once, as
  `sources: [{chunkId, text}]`. Each question carries its `sourceChunkIds`.
- **Answer key as context.** Each question carries `correctAnswers` (not judged), so the verifier knows how the
  question is meant and does not suggest a correct answer as a replacement.
- **Sources plus own knowledge.** The generator answers only from the sources. The verifier uses the sources and its
  own knowledge: facts specific to the document (numbers, extensions, internal rules) are decided by the sources, and
  general knowledge can also make a distractor correct.
- **Reason first, every answer.** The default prompt is `incorrect-options-verification`. It returns a verdict for
  every distractor, with `explanation` written **before** `verdict` (also in `verification-output-template.json`, so
  OpenAI structured output keeps that order). The mapper still flags only true verdicts.
- The reply no longer echoes `questionText` or `originalAnswer`.
- Verification output caps go to 2000 tokens (OpenAI and Ollama). The OpenAI verification timeout is 60 s. Ollama
  runs with a 600 s timeout, no retries and `num_ctx=8192`.
- Development (now the `local` profile) verifies with OpenAI `gpt-4.1` again; Ollama is an opt-in compose profile. The adapter stays available with
  `kg.testgen.verification.provider=ollama`.

This replaces the `incorrect-options-verification-only-flagged` default from ADR001. That template stays available
(deleted later by [ADR006](ADR006-restore-original-verification-prompt.md)).

## Context

Test 1 of the Corvane demo content (5 questions, chunks 262–275) showed the old verifier was unreliable:

- It saw only the question and the distractors. It could not judge document-specific facts (ext. 3333, a micro-break
  every 30 minutes), so it guessed.
- On CPU-only Ollama (about 7 tokens/s) the echoed text made replies of 490–640+ tokens. Runs timed out at 120 s or
  hit the 800-token cap, and a retry doubled the wait.
- The negatively phrased question "Which fire extinguisher should never be used on electrical equipment?" has a real
  second correct answer among the distractors: the source says foam must never be used on electrical equipment either.
  A good verifier flags exactly that one option.

Measured on that batch:

| Setup | Flags | Correct? |
|---|---|---|
| qwen2.5:7b, no sources, only-flagged | 5 | All wrong; some replacements were the correct answer |
| qwen2.5:7b, sources, only-flagged | 2–5 | All wrong, Foam missed |
| gpt-4.1, sources, only-flagged | 0 (twice) | Foam missed; the model returned `[]` without reasoning |
| gpt-4.1, sources, all verdicts, verdict before explanation | 2–3 | Foam found, plus CO2/fire blanket flagged with reasons for the opposite question |
| gpt-4.1, sources + answer key, all verdicts, **explanation first** | 1 (three runs) | Exactly Foam, with a source-based reason |
| qwen2.5:7b (CPU), same final prompt | 0 | No false flags any more, but Foam missed; explanations are boilerplate. 893 output tokens, 202 s |

## Options Considered

### Option 1 (SELECTED): Sources + answer key, every verdict, explanation first

#### Consequences
* Adopted because: it was the only variant that gave the right result, and it did so in every run.
* Adopted because: explanations make each flag reviewable, and the recorded run shows why a distractor passed.
* Adopted despite: longer replies (about 1250 output tokens for 5 questions instead of about 7–300). Cheap on OpenAI
  (about 5 s). On CPU-only Ollama this takes minutes per batch.
* Adopted despite: a larger prompt (about 1650 input tokens for 5 questions with their chunks).

### Option 2: Keep the only-flagged prompt, add sources

#### Consequences
* Rejected because: with nothing to write for false verdicts, gpt-4.1 returned an empty result without checking, and
  qwen2.5:7b flagged wrong options.
* Rejected despite: the smallest replies.

### Option 3: Judge by the sources only

#### Consequences
* Rejected because: a distractor that is true in general (not in the document) would pass unflagged, though a
  test-taker could reasonably pick it.

## Advice
* qwen2.5:7b-instruct does not judge distractors reliably, even with this prompt. Use OpenAI for verification when
  flag quality matters, or a larger local model if a GPU is available.

## Supporting Material
* [ADR001 - Review process](ADR001-review-process.md)
* [ADR002 - Local Ollama model for distractor verification](ADR002-local-verification-model.md)
* [Test generation](../architecture/06-test-generation.md)
