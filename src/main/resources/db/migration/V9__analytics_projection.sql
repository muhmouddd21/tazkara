CREATE TABLE analytics_event_sales (
    event_id      bigint      PRIMARY KEY,
    tickets_sold  bigint      NOT NULL,
    last_sale_at  timestamptz NOT NULL
);

CREATE TABLE analytics_processed_tickets (
    ticket_id     bigint      PRIMARY KEY      
);