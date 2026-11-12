# Demo content

Sources for the sample documents that the `dev` and `docker` profiles upload at startup (`DevDocumentSeeder`).

| Folder | Topic (`R__dev_seed_topics.sql`) | Content |
|---|---|---|
| `employee-onboarding/` | Employee Onboarding & Compliance (7000) | 15 workplace policies for **Corvane Systems**, a fictional software company |

All text is original and written for KnowGauge. Corvane Systems, its products, tools (TripDesk, ExpenseHub,
PeoplePortal, Corvane Vault and so on) and contacts are fictional. Email addresses use the reserved `.example` domain.
Each PDF ends with a disclaimer saying so.

## Writing guidelines

The documents are designed to produce good multiple-choice questions:

- Keep each document short (about 3 pages), so ingestion and test generation stay fast and cheap.
- Prefer concrete, checkable facts: thresholds, deadlines, roles, numbered steps, do/don't lists and short examples.
- Use values that are easy to confuse (1 hour vs 24 hours vs 72 hours, €50 vs €100 vs €150). They give the
  verifier realistic distractors to check.
- Keep shared facts consistent across documents: contacts (Security Desk ext. 4444, IT Service Desk ext. 2222,
  internal emergency ext. 3333), offices (Rotterdam, Lisbon, Kraków) and tool names.

## Format

Each file starts with front matter. `pdf` is the output file name in `resources/devdocs/`, and the other fields fill
the document header:

```
---
title: Code of Conduct
pdf: Corvane - Code of Conduct.pdf
docid: ETH-001
version: 4.0
effective: 1 January 2026
owner: Ethics & Compliance Office
appliesTo: All employees
---
```

The body supports a small Markdown subset: `#`–`###` headings, paragraphs, `-` and `1.` lists, tables, `>` quotes,
`**bold**` and `*italic*`.

## Building the PDFs

The PDFs are committed, so you only need this after editing a source. It needs Node.js and Microsoft Edge or Google
Chrome (set `KG_PDF_BROWSER` or pass `--browser <path>` if neither is found in the default location):

```bash
node demo-content/build-pdfs.mjs
```

To add a document, create the Markdown file, build the PDFs and add a `SeedDocument` entry in `DevDocumentSeeder`.
Never change the `originalFileName` of a document that is already seeded: it is the seeder's idempotency key.
