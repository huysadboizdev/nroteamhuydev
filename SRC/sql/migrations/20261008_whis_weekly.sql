-- Weekly Whis scores are separate from legacy player.thachdauwhis.
CREATE TABLE IF NOT EXISTS whis_weekly_state (
    id TINYINT NOT NULL PRIMARY KEY,
    week_start DATE NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS whis_weekly_score (
    week_start DATE NOT NULL,
    player_id BIGINT NOT NULL,
    wins INT NOT NULL DEFAULT 0,
    last_win BIGINT NOT NULL,
    PRIMARY KEY (week_start, player_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS whis_weekly_reward (
    reward_week DATE NOT NULL,
    player_id BIGINT NOT NULL,
    rank_no TINYINT NOT NULL,
    PRIMARY KEY (reward_week, player_id),
    UNIQUE KEY reward_rank (reward_week, rank_no)
) ENGINE=InnoDB;
