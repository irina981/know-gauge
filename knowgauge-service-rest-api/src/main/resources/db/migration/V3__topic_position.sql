-- Optional display order among sibling topics. Clients sort by position first (lowest first), then by name;
-- topics without a position come after the positioned ones.
ALTER TABLE topics ADD COLUMN position INTEGER NULL;
