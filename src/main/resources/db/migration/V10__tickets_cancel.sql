ALTER TABLE tickets
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PURCHASED',
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN cancellation_email_sent_at TIMESTAMPTZ;
    ADD COLUMN cancelled_by VARCHAR(255);