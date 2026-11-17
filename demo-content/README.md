# Demo content

Sources for the sample documents that the `local` profile uploads at startup (`DevDocumentSeeder`).

| Folder | Topic (`R__dev_seed_topics.sql`) | Content |
|---|---|---|
| `employee-onboarding/` | Corvane Systems › Employee Onboarding & Compliance (7000) | 15 workplace policies for **Corvane Systems**, a fictional software company |
| `office-guides/` | Corvane Systems › Office Guides (9000) | 3 office guides with the same 4 subjects each, written for **focused** tests |
| `gradovi-srbije/` | Gradovi Srbije (10000) | 4 Serbian city guides (Latin script) with the same 4 sections each; real facts |
| `ai-day-to-day/` | AI in Day-to-Day Work (11000) | 4 English guides on generative AI tools with the same 4 sections each |
| `english-lessons/` | English Lessons (B1–B2) (12000) | 4 English lessons for intermediate learners with the same 4 sections each |
| `algorithms/` | *Not seeded for now* (was Programming › Algorithms for Coding Interviews, 5000) | 4 documents of interview algorithm patterns; every pattern has idea, when to use it, complexity, sample tasks. Sources only; see `TODO.md` 3.1 |
| – | Programming › Java (2000) | 7 lessons provided as PDFs only (no source folder); see "Java" below |
| – | Programming › System Design (3000) | 5 lessons provided as PDFs only (no source folder); see "System Design" below |

Both Corvane sets sit under the root topic **Corvane Systems (demo company)** (6000).

All text is original and written for KnowGauge. Corvane Systems, its products, tools (TripDesk, ExpenseHub,
PeoplePortal, Corvane Vault and so on) and contacts are fictional. Email addresses use the reserved `.example` domain.
Each PDF ends with a disclaimer saying so. The city guides and the AI guides are not about Corvane: they give general,
real information, and their disclaimer says to check official sources.

Front matter can set `brand` (the line above the title, default Corvane Systems), `lang` (`en` or `sr`, for the
header labels) and `disclaimer`.

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

Add `--set <folder>` to build only one folder, for example `--set office-guides`. The other PDFs are then left
unchanged.

To add a document, create the Markdown file, build the PDFs and add a `SeedDocument` entry in `DevDocumentSeeder`.
Never change the `originalFileName` of a document that is already seeded: it is the seeder's idempotency key.

## Office Guides: material for focused tests

`FOCUSED` coverage takes only the passages close to a focus text instead of a balanced sample. The three office
guides (Rotterdam, Lisbon, Kraków) all have the same four sections, and the subjects are deliberately far apart, so it
is easy to see whether a question belongs to the focus. Generate from the **Office Guides** topic with a focus such
as:

| Focus text | Every question should be about |
|---|---|
| Fire evacuation, alarms, assembly points and fire wardens | Section 1 of each guide: stairwells, assembly points, warden vests, drills |
| Parking, electric car charging, bicycles and public transport | Section 2: parking spaces and booking, chargers, bike storage, travel allowances |
| Kitchen, coffee, fridge rules and team lunches | Section 3: free drinks, fridge clean-out days, dishwasher, lunches |
| Booking meeting rooms and registering visitors | Section 4: room check-in, booking limits, video-wall rooms, visitor badges |

Each focus selects 6 to 9 passages from all three guides. A focused test can ask up to two questions per passage,
so ask for **5 to 12 questions**: a focus with too few close passages fails with a clear message (including how many
questions would fit) instead of padding the test with unrelated material. Compare with a balanced test over the same
topic, whose questions mix all four subjects.

## Gradovi Srbije and AI in Day-to-Day Work

Both sets use the same four sections in every document, so a balanced test from the root topic covers all of them,
and a focused test on one section draws it from every document. Real-world subjects overlap more than the office
guides (landmarks are historic, festivals take place at landmarks), so some focus texts select more cleanly than
others. These were checked with the embedding model:

| Topic | Focus text | Selects |
|---|---|---|
| Gradovi Srbije | Hrana i piće, tradicionalna jela i rakija | Food sections of all four cities |
| Gradovi Srbije | Festivali i manifestacije: koncerti, film i pozorište | Festival sections of all four cities |
| Gradovi Srbije | Istorija grada: osnivanje, vladari, ratovi i oslobođenje | Mostly history, some landmarks |
| Gradovi Srbije | Znamenitosti za obilazak: tvrđave, spomenici, muzeji i parkovi | Mostly landmarks, some festivals |
| AI in Day-to-Day Work | Privacy and data security: personal data, GDPR and confidential information | Privacy sections of all four guides |
| AI in Day-to-Day Work | Risks and limitations: hallucinations, errors, deepfakes and copyright | Mostly risk sections |
| AI in Day-to-Day Work | How AI tools work: language models, tokens, prompts and training | Mostly "how it works" sections |
| AI in Day-to-Day Work | Deepfakes, voice cloning scams and labelling AI-generated media | The images and media guide only |

Generate Serbian tests with **Language: Serbian**.

## English Lessons (B1–B2)

Four lessons (travel, work, health, technology), each with vocabulary, one grammar point, common mistakes and useful
phrases. A focus on one grammar point selects exactly that lesson's grammar section; the section focuses select the
matching section from every lesson (checked with the embedding model):

| Focus text | Selects |
|---|---|
| Present perfect and past simple | The grammar section of Lesson 1 |
| The passive voice | The grammar section of Lesson 4 |
| Useful phrases for everyday situations | Useful phrases of all four lessons, plus some lesson introductions |
| Common mistakes and their corrections | Common mistakes of all four lessons, plus some lesson introductions |

Grammar focuses match only a few passages: ask for 3 to 5 questions.

## System Design

Programming › System Design (3000) is a five-lesson tutorial series provided as PDFs only (there is no Markdown
source here): System Design Fundamentals, Scalability and Performance, Data Storage and Consistency, Distributed
Systems and Messaging, and Reliability, Security, and Observability. Each lesson has four pages; all five are
documents directly in System Design, uploaded (and so listed) in lesson order.

Each focus text names a subject a whole test is realistically about, and selects enough passages for one
(checked with the embedding model):

| Focus text | Selects |
|---|---|
| Scalability and performance | Mostly Lesson 2, some of Lesson 1 (17 passages) |
| Caching and load balancing | Lesson 2 (11 passages) |
| Databases: storage, replication, sharding and consistency | Mostly Lesson 3 (17 passages) |
| Data consistency and transactions | Lessons 3 and 4 (15 passages) |
| Messaging and asynchronous communication between services | Lesson 4 (11 passages) |
| Reliability and fault tolerance | Mostly Lessons 5 and 4 (22 passages) |
| Security and observability of production systems | Mostly Lesson 5's security and observability pages (11 passages) |
| Capacity estimation and requirements | Lessons 1 and 2 (19 passages) |

The generate form offers these as suggestions (`DevFocusSuggestionsController`, dev profile only).

## Java

Programming › Java (2000) is a seven-lesson tutorial series provided as PDFs only, like System Design: Java Language
Fundamentals; Modern Java: Streams, Records, Optionals; JVM Memory and Garbage Collection; Processes, Threads, and
Java Concurrency; Spring Framework Fundamentals; Spring Boot Practical Guide; and Quarkus Fundamentals and
Architecture. Each lesson has four pages; all seven are documents directly in Java, in lesson order.

Each focus text names a subject a whole test is realistically about, and selects enough passages for one
(checked with the embedding model):

| Focus text | Selects |
|---|---|
| Threads, synchronization and concurrency in Java | Mostly the concurrency lesson (16 passages) |
| Memory management and garbage collection in the JVM | The JVM lesson (11 passages) |
| Classes, objects, inheritance and interfaces in Java | Mostly the fundamentals lesson (11 passages) |
| Lambdas, streams, records and Optional in modern Java | Mostly the modern Java lesson (20 passages) |
| Spring Framework and Spring Boot | The Spring and Spring Boot lessons (16 passages) |
| Dependency injection and beans in Spring | Spring and Spring Boot, plus Quarkus CDI (18 passages) |
| Transactions and database access | Spring, Spring Boot and Quarkus persistence pages (8 passages) |
| Web applications and REST endpoints | Spring MVC, Spring Boot and Quarkus REST (10 passages) |
| Monitoring, health checks and observability of Java services | JVM tooling, Actuator and Quarkus deployment (18 passages) |
| Quarkus and cloud-native Java | The Quarkus lesson (8 passages) |

The generate form offers these as suggestions (`DevFocusSuggestionsController`, dev profile only).
