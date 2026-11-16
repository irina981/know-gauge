---
title: AI Coding Assistants
pdf: AI at Work - Coding Assistants.pdf
brand: AI in Day-to-Day Work
docid: AI-01
version: 1.0
effective: November 2026
appliesTo: Developers and anyone who writes scripts or queries
disclaimer: Sample document created for the KnowGauge demo. It gives general information only; check your organisation's own rules before using AI tools at work.
---

# AI Coding Assistants

AI coding assistants suggest, explain and change code from inside the editor or a chat window. This guide covers
four subjects: how they work, good uses, risks and limitations, and privacy and data security.

## 1. How coding assistants work

### 1.1 Large language models trained on code

- Coding assistants are built on **large language models (LLMs)** trained on large amounts of text and **source
  code**.
- The model predicts the most likely next piece of text, called a **token**, one token at a time. A suggestion is a
  long chain of such predictions, not a lookup in a database of correct answers.

### 1.2 Context from the editor

- The assistant sends the model a **prompt** that contains your request plus **context**: the open file, nearby
  files and sometimes the error message or terminal output.
- The amount of text a model can consider at once is its **context window**. Code outside the context window is
  invisible to the model.
- **Agent** modes let the assistant run commands, read more files and edit several files in a loop until a task is
  done.

### 1.3 Knowledge cut-off

- A model only knows what was in its training data, up to its **knowledge cut-off** date.
- Libraries released or changed after that date may be unknown to the model unless their documentation is added
  to the context.

## 2. Good uses of coding assistants

### 2.1 Writing code faster

- **Boilerplate**: data classes, mappers, configuration files and repetitive tests.
- **Code completion**: finishing the current line or function from the surrounding code.
- **Translating** code between languages, for example from a shell script to Python.

### 2.2 Understanding code

- **Explaining** unfamiliar code, regular expressions or SQL queries in plain language.
- **Finding** where something is implemented in a large codebase.
- **Summarising** what a pull request changes before a code review.

### 2.3 Improving code

- Writing **unit tests** for existing code, including edge cases you did not think of.
- **Refactoring**: renaming, extracting methods and simplifying conditions.
- Suggesting a fix for a **failing test** or a stack trace.

## 3. Risks and limitations of coding assistants

### 3.1 Wrong but convincing code

- Assistants can **hallucinate**: invent functions, parameters or library versions that do not exist.
- Generated code can compile and still be **wrong**, for example by handling only the happy path.
- Every suggestion must be **reviewed and tested** like code from a new colleague.

### 3.2 Security and licensing

- Generated code can contain **security vulnerabilities**, such as SQL injection or missing input validation.
- Suggested **dependencies** may be outdated or, in rare cases, may not exist at all; attackers register such
  invented package names (**package hallucination** or "slopsquatting").
- Large verbatim copies of existing code can raise **licence** questions; many tools offer a filter that blocks
  suggestions matching public code.

### 3.3 Over-reliance

- Relying on suggestions for everything can slow down **learning** for junior developers.
- Agents that run commands must be watched: a wrong command can delete files or change data.

## 4. Privacy and data security with coding assistants

### 4.1 What leaves your machine

- Prompts and **code context are sent to the provider's servers**, unless the model runs locally.
- Check whether the provider may **store** prompts and use them for **training**. Business plans usually exclude
  training on customer code.

### 4.2 Secrets and personal data

- Never paste **secrets** (passwords, API keys, tokens, private keys) into a prompt.
- Exclude files with secrets, such as `.env` files, from the assistant's context.
- Do not use real **customer personal data** in prompts; use anonymised or test data.

### 4.3 Company rules

- Use only assistants **approved by your organisation**, with the company account rather than a private one.
- Follow the organisation's rules on which repositories may be used with AI tools.
