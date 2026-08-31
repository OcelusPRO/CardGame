-- Catalogue of official cards and aggregated usage statistics.
-- Live games are never stored here: they live in Redis and expire on their own.

CREATE TABLE card_packs (
    id                VARCHAR(64)  NOT NULL,
    name              VARCHAR(120) NOT NULL,
    description       TEXT         NOT NULL,
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at_millis BIGINT       NOT NULL,
    CONSTRAINT pk_card_packs PRIMARY KEY (id)
);

CREATE TABLE situation_cards (
    id                VARCHAR(64) NOT NULL,
    pack_id           VARCHAR(64) NOT NULL,
    text              TEXT        NOT NULL,
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at_millis BIGINT      NOT NULL,
    CONSTRAINT pk_situation_cards PRIMARY KEY (id),
    CONSTRAINT fk_situation_cards_pack FOREIGN KEY (pack_id) REFERENCES card_packs (id)
);

CREATE TABLE punchline_cards (
    id                VARCHAR(64) NOT NULL,
    pack_id           VARCHAR(64) NOT NULL,
    text              TEXT        NOT NULL,
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at_millis BIGINT      NOT NULL,
    CONSTRAINT pk_punchline_cards PRIMARY KEY (id),
    CONSTRAINT fk_punchline_cards_pack FOREIGN KEY (pack_id) REFERENCES card_packs (id)
);

-- How often a single card was played, voted for, and won a round.
CREATE TABLE card_usage (
    card_id VARCHAR(64) NOT NULL,
    kind    VARCHAR(16) NOT NULL,
    plays   BIGINT      NOT NULL DEFAULT 0,
    votes   BIGINT      NOT NULL DEFAULT 0,
    wins    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_card_usage PRIMARY KEY (card_id, kind)
);

-- Which punchline was played against which situation, and how well that pairing scored.
CREATE TABLE combo_stats (
    situation_id VARCHAR(64) NOT NULL,
    punchline_id VARCHAR(64) NOT NULL,
    plays        BIGINT      NOT NULL DEFAULT 0,
    votes        BIGINT      NOT NULL DEFAULT 0,
    wins         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_combo_stats PRIMARY KEY (situation_id, punchline_id)
);

CREATE TABLE daily_activity (
    activity_day   VARCHAR(10) NOT NULL,
    games_created  BIGINT      NOT NULL DEFAULT 0,
    rounds_played  BIGINT      NOT NULL DEFAULT 0,
    answers_played BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_daily_activity PRIMARY KEY (activity_day)
);

CREATE INDEX idx_situation_cards_pack ON situation_cards (pack_id, enabled);
CREATE INDEX idx_punchline_cards_pack ON punchline_cards (pack_id, enabled);
CREATE INDEX idx_card_usage_ranking ON card_usage (kind, plays);
CREATE INDEX idx_combo_stats_ranking ON combo_stats (votes);
