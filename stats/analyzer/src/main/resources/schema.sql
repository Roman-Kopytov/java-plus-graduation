DROP TABLE IF EXISTS user_action CASCADE;
DROP TABLE IF EXISTS event_similarity CASCADE;

CREATE TABLE IF NOT EXISTS user_action
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id     BIGINT NOT NULL,
    event_id    BIGINT NOT NULL,
    action_type VARCHAR,
    max_weight  DOUBLE PRECISION,
    timestamp  TIMESTAMP,
    CONSTRAINT uniqueId UNIQUE (user_id, event_id)
);

CREATE TABLE IF NOT EXISTS event_similarity
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    event_a    BIGINT NOT NULL,
    event_b    BIGINT NOT NULL,
    score     DOUBLE PRECISION,
    timestamp TIMESTAMP,
    CONSTRAINT uniqueAB UNIQUE (event_a, event_b)
);