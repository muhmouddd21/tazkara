ALTER TABLE analytics_processed_tickets
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PURCHASED';

