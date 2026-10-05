CREATE TABLE IF NOT EXISTS kol_reward_claim (
  player_id BIGINT NOT NULL,
  quest_version INT NOT NULL,
  branch TINYINT NOT NULL,
  quest_id TINYINT NOT NULL,
  account_id INT NOT NULL,
  reward_vnd INT NOT NULL,
  reward_points INT NOT NULL,
  claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (player_id, quest_version, branch, quest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wallet_debit_receipt (
  receipt_id CHAR(36) NOT NULL,
  account_id INT NOT NULL,
  debit_kind VARCHAR(32) NOT NULL,
  cost INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
