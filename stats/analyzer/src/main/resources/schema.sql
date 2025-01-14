DROP TABLE IF EXISTS user_action CASCADE;
DROP TABLE IF EXISTS event_similarity CASCADE;

CREATE TABLE IF NOT EXISTS user_action
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    userId     INT NOT NULL,
    eventId    INT NOT NULL,
    actionType VARCHAR,
    maxWeight  DOUBLE PRECISION,
    timestamp  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event_similarity
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    eventA    INT NOT NULL,
    eventB    INT NOT NULL,
    score     DOUBLE PRECISION,
    timestamp TIMESTAMP
);