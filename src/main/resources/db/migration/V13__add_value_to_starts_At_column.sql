UPDATE events
SET start_at = TIMESTAMPTZ '2027-01-01 00:00:00+00'
WHERE start_at IS NULL;

UPDATE events
SET end_at = start_at + INTERVAL '1 hour'
WHERE end_at IS NULL;

ALTER TABLE events
    ALTER COLUMN start_at SET NOT NULL,
    ALTER COLUMN end_at SET NOT NULL;