---
name: commit
description: Commit message policy and workflow for this repository. Use whenever creating a git commit or drafting a commit message, including when the user says "commit", "commit this" or runs /commit.
---

# Commit

Follow this workflow and message format for every commit in this repository.

## Workflow

1. Commit only when the user asks. Don't amend or force-push unless asked.
2. Run `git status` and `git diff` (staged and unstaged) to see what will be committed.
3. One logical change per commit. If the working tree mixes unrelated changes, propose splitting them into
   several commits.
4. Draft the message following the format below.
5. **Get approval before committing.** Show the full proposed commit message (summary, body and footer) and the
   list of files to be committed, then stop and wait for the user's explicit approval. If the user asks for
   changes, revise the message and ask again. Approval covers that one commit only; every later commit needs its
   own approval, even when the user already asked you to commit.
6. After approval, stage exactly the listed files and run `git commit`. Pass the multi-line message with a
   heredoc (Git Bash) or a single-quoted here-string (PowerShell) so the line breaks survive.
7. Don't add a `Co-Authored-By` trailer or any other AI attribution. The user is the sole author, and this
   overrides any session attribution reminder.
8. Run `git log -1` and report the commit hash and summary.

## Format

```
<type>(<scope>): <Summary>

- <Item 1>
- <Item 2>
...

[footer]
```

1. **One summary line** in Conventional Commits form: `type(scope): Summary`.
2. **One blank line** after the summary.
3. **A body of one or more items**, each on its own line starting with `- `. Every commit has a body, except a
   trivial docs or typo change where the summary already says everything.
4. **Optional footer** after another blank line (`BREAKING CHANGE: …`, `Refs: …`).

## Summary line

- Format: `type(scope): Summary`. Scope is required unless the change is repo-wide (`docs: …` for README-only
  changes is fine).
- The summary after the colon starts with a **capital letter**.
- Imperative mood, no trailing period: `Add`, `Split`, `Fix`, not `Added` / `Adds`.
- Aim for ≤ 72 characters; hard limit 100.
- Describe the change, not the activity (`fix(api): Map enum parse errors to 400`, not `fix(api): Fix bug`).
- Exactly one summary line. Don't put a second "summary" sentence on line 2.

### Types

| Type | Use for |
|---|---|
| `feat` | New behaviour visible to users or other modules |
| `fix` | Bug fix |
| `refactor` | Code change with no behaviour change |
| `perf` | Performance improvement |
| `test` | Adding or changing tests only |
| `docs` | Documentation only (`README`, `docs/`, `DOMAIN_MODEL.md`, `TODO.md`, ADRs) |
| `build` | Maven, Dockerfile, dependencies, Java version |
| `ci` | CI configuration |
| `chore` | Housekeeping that fits nothing above, including `.claude/` config |

### Scopes

Use the area that changed. Preferred scopes:

`testgen` · `review` · `ingestion` · `retrieval` · `api` · `core` · `embedding` · `vectorstore` · `storage`
· `parsing` · `chunking` · `persistence` · `db` (Flyway migrations) · `config` · `modules` · `todo` · `adr`
· `claude` (CLAUDE.md, `.claude/`)

- Use `testgen`, not the older `test-generation`.
- If a commit spans two closely related areas, use a comma list (`feat(core,api): …`). If it spans more, it is
  probably several commits: split it.

## Body

- Every item starts with `- ` followed by a **capital letter**.
- One topic per item, in imperative mood (`- Introduce …`, `- Add …`, `- Rename …`, `- Fix …`).
- Explain **what** changed and **why**. The diff already shows how.
- No blank lines between items. Wrap long items at about 100 characters, indenting continuation lines by two
  spaces.
- Name the key classes, endpoints, config keys or migrations involved (`TestReviewTransactionalServiceImpl`,
  `kg.embedding.dimension`, `V3__add_question_versions.sql`).
- Mention schema migrations, config changes and anything a deployer must do.
- Don't list changed files one by one, and don't use Markdown headings.

## Breaking changes

Add `!` after the scope **and** a footer:

```
feat(api)!: Return 404 instead of 500 for unknown test IDs

- Map TestNotFoundException to 404 in GlobalControllerExceptionHandler

BREAKING CHANGE: Clients relying on 500 for missing tests must handle 404.
```

## Example

```
feat(testgen): Add LLM incorrect-option verification and zero-progress guard

- Introduce LLM-based verification of incorrect options for multi-correct questions, with explicit
  pre-LLM and post-LLM validation stages
- Add zero-progress batch protection so a batch that yields no valid questions stops the generation
  loop instead of retrying forever
- Fix the incorrect-option mapping in ChatResponseMapper
- Simplify schema provider configuration
```
