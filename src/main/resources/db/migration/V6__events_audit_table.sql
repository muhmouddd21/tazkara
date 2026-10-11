CREATE TABLE events_audit (
    audit_id    bigserial PRIMARY KEY,
    event_id    bigint      NOT NULL,
    action      text        NOT NULL,
    old_data    jsonb       NOT NULL,
    changed_at  timestamptz NOT NULL DEFAULT now()
);

CREATE FUNCTION audit_events() RETURNS trigger AS $$
BEGIN
    INSERT INTO events_audit (event_id, action, old_data)
    VALUES (OLD.id, TG_OP, to_jsonb(OLD));
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_events_audit
    AFTER UPDATE OR DELETE ON events
    FOR EACH ROW EXECUTE FUNCTION audit_events();