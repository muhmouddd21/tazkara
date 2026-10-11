ALTER TABLE events
    ADD CONSTRAINT chk_events_total_seats
    CHECK (total_seats BETWEEN 1 AND 1000);