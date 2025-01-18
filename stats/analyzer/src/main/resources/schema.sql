DROP TABLE IF EXISTS user_action CASCADE;
DROP TABLE IF EXISTS event_similarity CASCADE;

CREATE TABLE IF NOT EXISTS user_action
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    userId     BIGINT NOT NULL,
    eventId    BIGINT NOT NULL,
    actionType VARCHAR,
    maxWeight  DOUBLE PRECISION,
    timestamp  TIMESTAMP,
    CONSTRAINT uniqueId UNIQUE (userId, eventId)
);

CREATE TABLE IF NOT EXISTS event_similarity
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    eventA    BIGINT NOT NULL,
    eventB    BIGINT NOT NULL,
    score     DOUBLE PRECISION,
    timestamp TIMESTAMP,
    CONSTRAINT uniqueAB UNIQUE (eventA, eventB)
);