# ADR002 - Local Ollama Model for Distractor Verification
* Date: Nov 12, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision

Distractor verification (the second LLM pass that flags wrong options that are actually true) can run on a local
Ollama model instead of OpenAI. `kg.testgen.verification.provider` selects the adapter for the
`LlmIncorrectOptionsVerificationService` port:

- `openai` (default in `application.properties`): `LlmIncorrectOptionsVerificationServiceImpl`, as before.
- `ollama` (set in the `dev` and `docker` profiles): `OllamaIncorrectOptionsVerificationServiceImpl`, using a
  general instruction model (default `qwen2.5:7b-instruct`) in Ollama's JSON mode.

The Ollama adapter sends the same prompt and parses the reply with the same `VerificationResponseMapper`. Core,
prompts and the response contract are unchanged. Generation stays on OpenAI.

## Context

Every generation batch makes one verification call to `gpt-4.1`. Verification is best effort and only flags
options (ADR001), so it is a good place to trade some accuracy for cost. The verifier gets the question text and
its distractors, but not the source chunks. It returns, for each distractor it judges true, a clearly false
replacement text and a reason. Review's "accept fix" action needs that replacement text.

## Options Considered

### Option 1 (SELECTED): General local instruction model with JSON output
Run the existing prompt on a local model such as `qwen2.5:7b-instruct` through `OllamaChatModel`.

#### Consequences
* Adopted because: no OpenAI tokens are spent on verification.
* Adopted because: it's a drop-in replacement. Flags, replacement text, reasons and accept fix all keep working,
  and core is unchanged.
* Adopted because: OpenAI stays available through one setting.
* Adopted despite: a 7B model judging from general knowledge is less reliable than `gpt-4.1`, so it may miss true
  distractors or add false flags. Flags never change the answer key, and a reviewer resolves each one.
* Adopted despite: it needs a running Ollama server and a pulled model (about 4.7 GB). If Ollama is unreachable,
  the run is recorded as failed and the batch continues unverified.

### Option 2: Dedicated fact-check model (`bespoke-minicheck`)
A grounding checker that answers Yes/No to "is this claim supported by this document".

#### Consequences
* Rejected because: it needs the source text, so chunk texts would have to go through the core port.
* Rejected because: it gives no replacement text or reason, so accept fix would not be available for its flags.
* Rejected because: it needs one call per distractor, and each question-plus-option has to be rewritten as a
  standalone claim, which is error-prone for "Which is NOT…" questions.
* Rejected despite: it checks distractors against the uploaded documents, which is arguably the right notion of
  correctness for a RAG product.

### Option 3: MiniCheck flags + general model writes replacements
MiniCheck flags against the chunks, then a general model writes replacement text only for the flagged options.

#### Consequences
* Rejected because: it needs the most work (core port change plus two adapters).
* Rejected despite: it would give grounded flags while keeping accept fix. Kept as a possible follow-up if the
  general model's accuracy proves insufficient.

## Supporting Material
* [06 – Test generation](../architecture/06-test-generation.md)
* [ADR001 – Test review process](ADR001-review-process.md)
