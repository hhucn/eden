CREATE SCHEMA IF NOT EXISTS aggregator
AUTHORIZATION aggregator;

CREATE TABLE IF NOT EXISTS aggregator.statements (
       id SERIAL PRIMARY KEY,
       author text NOT NULL,
       content text NOT NULL,
       aggregate_id text NOT NULL,
       entity_id text NOT NULL,
       version integer NOT NULL DEFAULT 1,
       ancestor_aggregate_id text,
       ancestor_entity_id text,
       ancestor_version integer,
       created TIMESTAMP NOT NULL DEFAULT now()
)
WITH (
     OIDS = FALSE
)
TABLESPACE pg_default;

CREATE INDEX on aggregator.statements (id);
CREATE INDEX on aggregator.statements (entity_id);
CREATE INDEX on aggregator.statements (aggregate_id);

ALTER TABLE aggregator.statements OWNER to aggregator;


CREATE TABLE IF NOT EXISTS aggregator.links (
       id SERIAL PRIMARY KEY,
       author text NOT NULL,
       type text NOT NULL,
       aggregate_id text NOT NULL,
       entity_id text NOT NULL,
       from_aggregate_id text NOT NULL,
       from_entity_id text NOT NULL,
       from_version integer NOT NULL DEFAULT 1,
       to_aggregate_id text NOT NULL,
       to_entity_id text NOT NULL,
       to_version integer,
       created TIMESTAMP NOT NULL DEFAULT now()
)
WITH (
     OIDS = FALSE
)
TABLESPACE pg_default;

CREATE INDEX on aggregator.links (id);
CREATE INDEX on aggregator.links (entity_id);
CREATE INDEX on aggregator.links (aggregate_id);

ALTER TABLE aggregator.links OWNER to aggregator;

