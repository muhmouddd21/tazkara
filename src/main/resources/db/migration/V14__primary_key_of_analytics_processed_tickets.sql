ALTER TABLE analytics_processed_tickets
    DROP CONSTRAINT analytics_processed_tickets_pkey;

ALTER TABLE analytics_processed_tickets
ADD CONSTRAINT analytics_processed_tickets_pkey
PRIMARY KEY (ticket_id);