CREATE SCHEMA IF NOT EXISTS aggregator
AUTHORIZATION aggregator;

CREATE TABLE IF NOT EXISTS aggregator.events (
       sequence_number SERIAL PRIMARY KEY,
       version smallint NOT NULL DEFAULT 1,
       aggregate_id text,
       entity_id text,
       creator text NOT NULL,
       created  TIMESTAMP NOT NULL DEFAULT now(),
       type text NOT NULL,
       data text
)
WITH (
     OIDS = FALSE
)
TABLESPACE pg_default;

CREATE INDEX on aggregator.events (aggregate_id);
CREATE INDEX on aggregator.events (creator);
CREATE INDEX on aggregator.events (type);


ALTER TABLE aggregator.events OWNER to aggregator;
