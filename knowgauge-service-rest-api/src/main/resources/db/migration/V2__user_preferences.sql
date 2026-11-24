-- Settings a user chose for the app (theme, ...), as JSON so new settings need no migration. NULL until the user
-- changes one; the app then uses its defaults.
ALTER TABLE users ADD COLUMN preferences_json JSONB NULL;
