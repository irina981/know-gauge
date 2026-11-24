-- A workspace may have its own configuration values too (ADR014), not only its own limits: the models, questions per
-- LLM call, and the default question count and difficulty of new tests. Test generation uses a workspace's own value
-- where it has one, else the system value. The ingestion batch size stays system-only: the scheduled ingestion job
-- serves every workspace at once.
INSERT INTO setting_config_owners (setting_key, setting_owner)
SELECT setting_key, 'TENANT' FROM setting_configs
WHERE setting_type = 'CONFIG'
  AND setting_key IN ('kg.testgen.defaults.generation-model',
                      'kg.testgen.verification.chat-model.openai.model',
                      'kg.testgen.defaults.question-generation-batch-size',
                      'kg.testgen.defaults.question-count',
                      'kg.testgen.defaults.difficulty')
ON CONFLICT DO NOTHING;
