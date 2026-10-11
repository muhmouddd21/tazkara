CREATE UNIQUE INDEX uq_events_name_city ON events (lower(name), lower(city));
