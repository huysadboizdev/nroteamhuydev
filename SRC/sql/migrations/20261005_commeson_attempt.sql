CREATE TABLE IF NOT EXISTS commeson_attempt (
 player_id BIGINT NOT NULL,
 attempt_day DATE NOT NULL,
 match_id VARCHAR(36) NOT NULL,
 state VARCHAR(12) NOT NULL,
 deadline BIGINT NOT NULL,
 PRIMARY KEY (player_id,attempt_day),
 UNIQUE KEY uq_commeson_match (match_id),
 KEY ix_commeson_pending (player_id,state)
) ENGINE=InnoDB;
