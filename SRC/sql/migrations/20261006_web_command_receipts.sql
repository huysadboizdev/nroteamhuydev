-- Additive migration. Existing completed command history is never replayed.
ALTER TABLE account ADD COLUMN IF NOT EXISTS admin INT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS server_command_queue (
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 command_type VARCHAR(50) NOT NULL,
 payload TEXT NULL,
 status ENUM('pending','processing','completed','failed') NOT NULL DEFAULT 'pending',
 created_by VARCHAR(50) DEFAULT 'admin',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 executed_at TIMESTAMP NULL,
 result_message VARCHAR(255) NULL,
 KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE server_command_queue ADD COLUMN IF NOT EXISTS request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL;
ALTER TABLE server_command_queue ADD COLUMN IF NOT EXISTS actor_account_id INT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_server_command_request ON server_command_queue(request_id);
CREATE TABLE IF NOT EXISTS admin_command_receipt (
 command_id BIGINT NOT NULL PRIMARY KEY,
 command_type VARCHAR(64) NOT NULL,
 player_id BIGINT NOT NULL DEFAULT 0,
 account_id INT NOT NULL DEFAULT 0,
 result_message TEXT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_admin_receipt_player(player_id),
 KEY idx_admin_receipt_account(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
