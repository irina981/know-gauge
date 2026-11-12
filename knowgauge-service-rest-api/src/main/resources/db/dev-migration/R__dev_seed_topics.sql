-- R__dev_seed_topics.sql
-- Dev / demo seed: topic tree for tenant 1 (rerunnable). Every leaf topic gets documents from DevDocumentSeeder,
-- so the tree has no empty topics.
--
-- Knowledge Library (1000)
-- ├── Programming (1100)
-- │   ├── Java (2000): Core Java (2100), Multithreading (2200)
-- │   └── Spring & Spring Boot (4000)
-- └── Employee Onboarding & Compliance (7000): fictional "Corvane Systems" policy pack (7100-7600)
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
  -- Root
  (1000, NULL, 1, 'Knowledge Library', 'Sample learning content for trying out KnowGauge', '/1000', 1, 1, now(), now()),

  -- Programming
  (1100, 1000, 1, 'Programming', 'Software development languages and frameworks', '/1000/1100', 1, 1, now(), now()),
  (2000, 1100, 1, 'Java', 'The Java language and platform', '/1000/1100/2000', 1, 1, now(), now()),
  (2100, 2000, 1, 'Core Java', 'Language fundamentals and object-oriented programming', '/1000/1100/2000/2100', 1, 1, now(), now()),
  (2200, 2000, 1, 'Multithreading', 'Processes, threads and concurrency in Java', '/1000/1100/2000/2200', 1, 1, now(), now()),
  (4000, 1100, 1, 'Spring & Spring Boot', 'Dependency injection and bean configuration in Spring', '/1000/1100/4000', 1, 1, now(), now()),

  -- Employee Onboarding & Compliance (fictional company: Corvane Systems)
  (7000, 1000, 1, 'Employee Onboarding & Compliance', 'Workplace policy training for new and existing employees of the fictional company Corvane Systems', '/1000/7000', 1, 1, now(), now()),
  (7100, 7000, 1, 'Code of Conduct & Ethics', 'Code of Conduct, conflicts of interest, gifts, hospitality and anti-bribery', '/1000/7000/7100', 1, 1, now(), now()),
  (7200, 7000, 1, 'Information Security', 'Acceptable use, passwords and MFA, phishing, data classification', '/1000/7000/7200', 1, 1, now(), now()),
  (7300, 7000, 1, 'Data Privacy (GDPR Basics)', 'Personal data, GDPR principles, data subject rights and breach response', '/1000/7000/7300', 1, 1, now(), now()),
  (7400, 7000, 1, 'Respect at Work', 'Anti-harassment, equal opportunity and speaking up', '/1000/7000/7400', 1, 1, now(), now()),
  (7500, 7000, 1, 'Health & Safety', 'Fire evacuation, office safety, ergonomics and home workstations', '/1000/7000/7500', 1, 1, now(), now()),
  (7600, 7000, 1, 'Working at Corvane', 'Remote and hybrid work, travel and expenses, leave and time off', '/1000/7000/7600', 1, 1, now(), now())
ON CONFLICT (id) DO UPDATE
SET parent_id   = EXCLUDED.parent_id,
    name        = EXCLUDED.name,
    description = EXCLUDED.description,
    path        = EXCLUDED.path,
    updated_by  = 1,
    updated_at  = now();

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

-- Seed documents were renamed to the "<Area> - <title>.pdf" scheme. Rename them in existing databases too,
-- because DevDocumentSeeder uses (topic_id, original_file_name) to skip documents it already uploaded.
UPDATE documents d
SET original_file_name = r.new_name,
    title              = r.new_title,
    updated_at         = now()
FROM (VALUES
  (2100, 'Java - Notes.pdf',                            'Java - Core Java Notes.pdf',              'Core Java Notes'),
  (2200, 'Java Multithreading - Process vs Thread.pdf', 'Java - Processes vs Threads.pdf',         'Processes vs Threads'),
  (4000, 'Spring - DI Types',                           'Spring - Dependency Injection Types.pdf', 'Dependency Injection Types'),
  (4000, 'Spring - Bean Configuration Types.pdf',       'Spring - Bean Configuration Types.pdf',   'Bean Configuration Types')
) AS r(topic_id, old_name, new_name, new_title)
WHERE d.topic_id = r.topic_id
  AND d.original_file_name = r.old_name
  AND (d.original_file_name <> r.new_name OR d.title IS DISTINCT FROM r.new_title)
  AND NOT EXISTS (
    SELECT 1 FROM documents x
    WHERE x.topic_id = r.topic_id AND x.original_file_name = r.new_name AND x.id <> d.id
  );

-- Topics that are no longer part of the seed. Remove them from existing databases, but only while nothing
-- (documents, tests, child topics outside this list) is attached to them. Leaves go first, then 6000.
DELETE FROM topics t
WHERE t.id IN (2300, 3000, 5000, 6100, 6200)
  AND NOT EXISTS (SELECT 1 FROM topics c WHERE c.parent_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.topic_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM test_covered_topics tc WHERE tc.topic_id = t.id);

DELETE FROM topics t
WHERE t.id = 6000
  AND NOT EXISTS (SELECT 1 FROM topics c WHERE c.parent_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.topic_id = t.id)
  AND NOT EXISTS (SELECT 1 FROM test_covered_topics tc WHERE tc.topic_id = t.id);
