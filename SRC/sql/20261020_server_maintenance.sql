-- Apply to the game/web shared database before starting the updated server.
-- Additive and repeatable; INSERT IGNORE preserves maintenance across restarts.
CREATE TABLE IF NOT EXISTS server_maintenance_state (
 id TINYINT NOT NULL PRIMARY KEY,
 phase VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 revision BIGINT NOT NULL DEFAULT 0,
 command_id BIGINT NULL,
 server_run VARCHAR(64) NOT NULL DEFAULT '',
 heartbeat_at TIMESTAMP NULL DEFAULT NULL,
 message VARCHAR(255) NOT NULL DEFAULT '',
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO server_maintenance_state(id,phase,message) VALUES(1,'OPEN','Chưa có yêu cầu bảo trì');
