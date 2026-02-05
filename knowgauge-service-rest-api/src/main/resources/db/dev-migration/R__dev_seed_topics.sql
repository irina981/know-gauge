-- R__dev_seed_topics.sql
-- Dev seed: topics (rerunnable)
-- Assumptions:
--  - topics table supports a tree via parent_id (nullable)
--  - ids are BIGINT and we can insert fixed ids for stable references
-- Audit columns:
--   created_by = 1
--   updated_by = 1
--   created_at = now()
--   updated_at = now()

-- =========================
-- TOPICS
-- =========================

INSERT INTO topics (
  id,
  parent_id,
  name,
  description,
  path,
  created_by,
  updated_by,
  created_at,
  updated_at
)
VALUES
  (1000, NULL, 'KB', 'Knowledge Base', '/1000', 1, 1, now(), now()),
  (2000, 1000, 'Java', 'Java core + ecosystem', '/1000/2000', 1, 1, now(), now()),
  (2100, 2000, 'Core Java', 'Language fundamentals', '/1000/2000/2100', 1, 1, now(), now()),
  (2200, 2000, 'Multithreading', 'Java multithreading', '/1000/2000/2200', 1, 1, now(), now()),
  (2300, 2000, 'Java Design Patterns', 'Java Design Patterns', '/1000/2000/2300', 1, 1, now(), now()),
  (3000, 1000, 'General', 'General topics', '/1000/3000', 1, 1, now(), now()),
  (4000, 1000, 'Spring/Spring Boot', 'Spring + Spring Boot', '/1000/4000', 1, 1, now(), now()),
  (5000, 1000, 'System Design', 'System Design', '/1000/5000', 1, 1, now(), now()),
  (6000, 1000, 'Databases', 'SQL + internals', '/1000/6000', 1, 1, now(), now()),
  (6100, 6000, 'PostgreSQL', 'Postgres features', '/1000/6000/6100', 1, 1, now(), now()),
  (6200, 6000, 'pgvector', 'Vector search in Postgres', '/1000/6000/6200', 1, 1, now(), now())
ON CONFLICT (id) DO UPDATE
SET parent_id   = EXCLUDED.parent_id,
    name        = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_by  = 1,
    updated_at  = now();
