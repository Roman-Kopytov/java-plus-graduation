DROP TABLE IF EXISTS requests CASCADE;

CREATE TABLE IF NOT EXISTS requests
(
    request_id   BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    created      TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    event_id     BIGINT                                  NOT NULL,
    requester_id BIGINT                                  NOT NULL,
    status       CHARACTER VARYING(100)                  NOT NULL,
    CONSTRAINT unique_requester_event UNIQUE (request_id, event_id)
);
