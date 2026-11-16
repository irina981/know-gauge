-- FOCUSED coverage: the questions are generated from the chunks most similar to this text (pgvector cosine
-- similarity). Required for FOCUSED, NULL for the balanced modes.
ALTER TABLE tests ADD COLUMN focus_query TEXT NULL;

ALTER TABLE tests ADD CONSTRAINT chk_tests_focus_query CHECK (
    coverage_mode <> 'FOCUSED' OR (focus_query IS NOT NULL AND btrim(focus_query) <> '')
);
