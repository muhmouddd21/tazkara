UPDATE analytics_processed_tickets
SET status = 'PURCHASED'
WHERE status IS NULL;

ALTER TABLE analytics_processed_tickets
ALTER COLUMN status SET DEFAULT 'PURCHASED';

ALTER TABLE analytics_processed_tickets
ALTER COLUMN status SET NOT NULL;