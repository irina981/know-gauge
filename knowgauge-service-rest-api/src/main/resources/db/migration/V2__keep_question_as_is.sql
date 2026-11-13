-- ADR008: a reviewer can keep a flagged question as it is ("Keep as is"). This creates a version with origin
-- FLAGS_DISMISSED and a FLAGS_DISMISSED event, so both CHECK constraints get the new value.

ALTER TABLE test_question_versions DROP CONSTRAINT chk_tq_versions_origin;
ALTER TABLE test_question_versions ADD CONSTRAINT chk_tq_versions_origin
    CHECK (origin IN ('GENERATED', 'REGENERATED', 'FIX_ACCEPTED', 'MANUAL', 'FLAGS_DISMISSED'));

ALTER TABLE test_events DROP CONSTRAINT chk_test_events_type;
ALTER TABLE test_events ADD CONSTRAINT chk_test_events_type CHECK (event_type IN (
    'TEST_CREATED', 'TEST_GENERATED', 'TEST_REQUIRES_REVIEW', 'TEST_REVIEWED', 'TEST_FAILED',
    'TEST_PUBLISHED', 'TEST_UNPUBLISHED', 'QUESTION_GENERATED', 'OPTION_FLAGGED', 'FIX_ACCEPTED', 'QUESTION_REGENERATED',
    'REGENERATION_FAILED', 'QUESTION_MANUALLY_REPLACED', 'FLAGS_DISMISSED'));
