ALTER TABLE events 
ADD COLUMN start_at TIMESTAMPTZ,
ADD COLUMN end_at TIMESTAMPTZ;

ALTER TABLE events
    ADD CONSTRAINT chk_events_start_before_end
    CHECK (
        start_at IS NULL
        OR end_at IS NULL
        OR end_at > start_at
    );