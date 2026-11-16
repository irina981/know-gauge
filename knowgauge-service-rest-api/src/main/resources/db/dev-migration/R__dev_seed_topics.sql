-- R__dev_seed_topics.sql
-- Dev / demo seed: topic tree for tenant 1 (rerunnable). Every leaf topic gets documents from DevDocumentSeeder,
-- so the tree has no empty topics. The two lesson series (Java, System Design) keep their lessons directly in the
-- topic, listed in lesson order.
--
-- Five root topics (display order set by position, see the UPDATE below):
-- Programming (1100)
-- ├── Java (2000): seven lessons, from Java Language Fundamentals to Quarkus, directly in the topic
-- └── System Design (3000): five lessons, from System Design Fundamentals to Reliability, Security, and
--     Observability, directly in the topic
-- Corvane Systems (demo company) (6000): all content about the invented company
-- ├── Employee Onboarding & Compliance (7000): policy pack (7100-7600)
-- └── Office Guides (9000): one guide per office, Rotterdam (9100), Lisbon (9200), Kraków (9300), for FOCUSED tests
-- Gradovi Srbije (10000): Serbian (Latin script) city guides, Beograd (10100), Novi Sad (10200), Niš (10300),
--   Kragujevac (10400); real, well-known facts
-- AI in Day-to-Day Work (11000): English guides, Coding Assistants (11100), Writing and Translation (11200),
--   Chatbots (11300), Images, Audio and Video (11400)
-- English Lessons (B1–B2) (12000): Lesson 1 Travel (12100), Lesson 2 Work (12200), Lesson 3 Health (12300),
--   Lesson 4 Technology (12400)
--
-- Every multi-document set uses the same sections in each document, so a balanced test covers all of them and a
-- FOCUSED test on one section picks it from every document (see demo-content/README.md).
--
-- Notes:
--  - Fixed ids keep the references in DevDocumentSeeder stable.
--  - path is '/' + ancestor ids + own id, and is updated on conflict so re-parented topics stay consistent.
--  - Audit columns: created_by = updated_by = 1, timestamps = now().

INSERT INTO topics (
  id,
  parent_id,
  tenant_id,
  name,
  description,
  path,
  created_by,
  updated_by,
  created_at,
  updated_at
)
VALUES
  -- Programming (root)
  (1100, NULL, 1, 'Programming', 'Software development languages and frameworks', '/1100', 1, 1, now(), now()),
  -- Java and System Design: tutorial series provided as PDFs only (no Markdown source), one document per lesson.
  (2000, 1100, 1, 'Java', 'A tutorial series in seven lessons: the language, modern Java, the JVM, concurrency, Spring, Spring Boot and Quarkus', '/1100/2000', 1, 1, now(), now()),
  (3000, 1100, 1, 'System Design', 'A practical tutorial series in five lessons, from fundamentals to reliability, security and observability', '/1100/3000', 1, 1, now(), now()),

  -- Corvane Systems (root): an invented software company. Everything below is fictional demo content.
  (6000, NULL, 1, 'Corvane Systems (demo company)', 'Demo content about Corvane Systems, an invented software company: its workplace policies and office guides. All names, places, numbers and contacts are fictional.', '/6000', 1, 1, now(), now()),

  -- Employee Onboarding & Compliance
  (7000, 6000, 1, 'Employee Onboarding & Compliance', 'Workplace policy training for new and existing Corvane employees', '/6000/7000', 1, 1, now(), now()),
  (7100, 7000, 1, 'Code of Conduct & Ethics', 'Code of Conduct, conflicts of interest, gifts, hospitality and anti-bribery', '/6000/7000/7100', 1, 1, now(), now()),
  (7200, 7000, 1, 'Information Security', 'Acceptable use, passwords and MFA, phishing, data classification', '/6000/7000/7200', 1, 1, now(), now()),
  (7300, 7000, 1, 'Data Privacy (GDPR Basics)', 'Personal data, GDPR principles, data subject rights and breach response', '/6000/7000/7300', 1, 1, now(), now()),
  (7400, 7000, 1, 'Respect at Work', 'Anti-harassment, equal opportunity and speaking up', '/6000/7000/7400', 1, 1, now(), now()),
  (7500, 7000, 1, 'Health & Safety', 'Fire evacuation, office safety, ergonomics and home workstations', '/6000/7000/7500', 1, 1, now(), now()),
  (7600, 7000, 1, 'Working at Corvane', 'Remote and hybrid work, travel and expenses, leave and time off', '/6000/7000/7600', 1, 1, now(), now()),

  -- Office Guides (for FOCUSED tests). Every guide has the same four clearly different sections: fire evacuation,
  -- parking and commuting, kitchen and coffee, meeting rooms and visitors.
  (9000, 6000, 1, 'Office Guides', 'Practical guides to the Corvane offices: fire evacuation, parking, kitchen and meeting rooms', '/6000/9000', 1, 1, now(), now()),
  (9100, 9000, 1, 'Rotterdam Office', 'Corvane headquarters in the Maaskade Tower', '/6000/9000/9100', 1, 1, now(), now()),
  (9200, 9000, 1, 'Lisbon Office', 'Corvane office in the Casa do Rio building', '/6000/9000/9200', 1, 1, now(), now()),
  (9300, 9000, 1, 'Kraków Office', 'Corvane office in the Wawel Business Park', '/6000/9000/9300', 1, 1, now(), now()),

  -- Gradovi Srbije (root; Serbian, Latin script). Every city guide has the same four sections: istorija,
  -- znamenitosti, hrana i piće, manifestacije. Real, well-known facts.
  (10000, NULL, 1, 'Gradovi Srbije', 'Vodiči kroz gradove Srbije: istorija, znamenitosti, hrana i piće, manifestacije', '/10000', 1, 1, now(), now()),
  (10100, 10000, 1, 'Beograd', 'Glavni grad Srbije, na ušću Save u Dunav', '/10000/10100', 1, 1, now(), now()),
  (10200, 10000, 1, 'Novi Sad', 'Centar Vojvodine, na Dunavu, ispod Fruške gore', '/10000/10200', 1, 1, now(), now()),
  (10300, 10000, 1, 'Niš', 'Najveći grad na jugu Srbije, na Nišavi', '/10000/10300', 1, 1, now(), now()),
  (10400, 10000, 1, 'Kragujevac', 'Najveći grad Šumadije, prva prestonica moderne Srbije', '/10000/10400', 1, 1, now(), now()),

  -- AI in Day-to-Day Work (root; English). Every guide has the same four sections: how it works, good uses, risks
  -- and limitations, privacy and data security. General information, no product versions or prices.
  (11000, NULL, 1, 'AI in Day-to-Day Work', 'Using generative AI tools at work: how they work, good uses, risks, and privacy', '/11000', 1, 1, now(), now()),
  (11100, 11000, 1, 'Coding Assistants', 'AI assistants that suggest, explain and change code', '/11000/11100', 1, 1, now(), now()),
  (11200, 11000, 1, 'Writing and Translation', 'AI for drafting, editing and translating text', '/11000/11200', 1, 1, now(), now()),
  (11300, 11000, 1, 'Chatbots', 'AI chatbots for customer service and internal help desks', '/11000/11300', 1, 1, now(), now()),
  (11400, 11000, 1, 'Images, Audio and Video', 'Generative AI for images, voices and video', '/11000/11400', 1, 1, now(), now()),

  -- English Lessons (root; B1-B2 learners). Every lesson has the same four sections: vocabulary, grammar (one point
  -- per lesson), common mistakes, useful phrases.
  (12000, NULL, 1, 'English Lessons (B1–B2)', 'English lessons for intermediate learners: vocabulary, grammar, common mistakes and useful phrases', '/12000', 1, 1, now(), now()),
  (12100, 12000, 1, 'Lesson 1 – Travel and Holidays', 'Travel vocabulary; present perfect and past simple', '/12000/12100', 1, 1, now(), now()),
  (12200, 12000, 1, 'Lesson 2 – Work and Careers', 'Work vocabulary; modal verbs for obligation and advice', '/12000/12200', 1, 1, now(), now()),
  (12300, 12000, 1, 'Lesson 3 – Health and Lifestyle', 'Health vocabulary; first and second conditional', '/12000/12300', 1, 1, now(), now()),
  (12400, 12000, 1, 'Lesson 4 – Technology and the Internet', 'Technology vocabulary; the passive voice', '/12000/12400', 1, 1, now(), now())
ON CONFLICT (id) DO UPDATE
SET parent_id   = EXCLUDED.parent_id,
    name        = EXCLUDED.name,
    description = EXCLUDED.description,
    path        = EXCLUDED.path,
    updated_by  = 1,
    updated_at  = now();

-- Display order of the root topics (V3__topic_position.sql). Other topics have no position and sort by name.
UPDATE topics
SET position = CASE id
                 WHEN 6000 THEN 1   -- Corvane Systems (demo company)
                 WHEN 11000 THEN 2  -- AI in Day-to-Day Work
                 WHEN 12000 THEN 3  -- English Lessons (B1–B2)
                 WHEN 1100 THEN 4   -- Programming
                 WHEN 10000 THEN 5  -- Gradovi Srbije
               END
WHERE id IN (6000, 11000, 12000, 1100, 10000);

-- Re-parenting seed topics changes their paths. Rebuild the paths of all their descendants (including topics
-- created by users under a seed topic) from parent_id, so path-prefix queries keep working.
WITH RECURSIVE tree AS (
  SELECT id, '/' || id AS path FROM topics WHERE parent_id IS NULL
  UNION ALL
  SELECT c.id, tree.path || '/' || c.id FROM topics c JOIN tree ON c.parent_id = tree.id
)
UPDATE topics t
SET path = tree.path
FROM tree
WHERE t.id = tree.id
  AND t.path IS DISTINCT FROM tree.path;

-- Topics that are no longer part of the seed. Remove them from existing databases, but only while nothing
-- (documents, tests, child topics outside this list) is attached to them. Leaves go first, then 5000 and 6000.
-- Algorithms for Coding Interviews (5000-5400) was removed for now; its sources stay in demo-content/algorithms.
-- The first System Design sub-topics (3100-3300) were replaced by five lesson sub-topics (3010-3050), and those by
-- the lessons directly in System Design. The Java sub-topics Core Java (2100), Multithreading (2200) and Spring &
-- Spring Boot (4000) were replaced by the seven lessons directly in Java. Sub-topics that still hold documents
-- stay until they are deleted in the Library (which also removes their passages from the vector store).
DELETE FROM topics t
WHERE t.id IN (2100, 2200, 2300, 3010, 3020, 3030, 3040, 3050, 3100, 3200, 3300, 4000, 5100, 5200, 5300, 5400, 6100, 6200)
  AND NOT EXISTS (SELECT 1 FROM topics c WHERE c.parent_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.topic_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM test_covered_topics tc WHERE tc.topic_id = t.id);

DELETE FROM topics t
WHERE t.id IN (5000, 6000)
  AND NOT EXISTS (SELECT 1 FROM topics c WHERE c.parent_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.topic_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM test_covered_topics tc WHERE tc.topic_id = t.id);

-- The former root "Knowledge Library" (1000). Programming and Employee Onboarding & Compliance were moved to the
-- top level above, so it is empty unless a user added topics, documents or tests to it; then it stays.
DELETE FROM topics t
WHERE t.id = 1000
  AND NOT EXISTS (SELECT 1 FROM topics c WHERE c.parent_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.topic_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM test_covered_topics tc WHERE tc.topic_id = t.id);
