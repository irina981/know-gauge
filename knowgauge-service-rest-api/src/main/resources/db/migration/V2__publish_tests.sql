-- ADR005: a PUBLISHED test is frozen (no review actions) and is the only status that accepts attempts.

ALTER TABLE tests DROP CONSTRAINT chk_tests_status;
ALTER TABLE tests ADD CONSTRAINT chk_tests_status
    CHECK (status IN ('CREATED', 'GENERATED', 'REQUIRES_REVIEW', 'REVIEWED', 'FAILED', 'PUBLISHED'));

ALTER TABLE test_events DROP CONSTRAINT chk_test_events_type;
ALTER TABLE test_events ADD CONSTRAINT chk_test_events_type CHECK (event_type IN (
    'TEST_CREATED', 'TEST_GENERATED', 'TEST_REQUIRES_REVIEW', 'TEST_REVIEWED', 'TEST_FAILED', 'TEST_PUBLISHED',
    'QUESTION_GENERATED', 'OPTION_FLAGGED', 'FIX_ACCEPTED', 'QUESTION_REGENERATED',
    'REGENERATION_FAILED', 'QUESTION_MANUALLY_REPLACED'));
