-- BOSS_NOTICE_BOARD_20261005
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS boss_notice_history (
 spawn_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 server_run CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 boss_id BIGINT NOT NULL,form_id INT NOT NULL,boss_name VARCHAR(100) NOT NULL,
 map_name VARCHAR(100) NOT NULL,appeared_at BIGINT NOT NULL,defeated_at BIGINT NOT NULL DEFAULT 0,
 killer_name VARCHAR(80) NOT NULL DEFAULT '',status VARCHAR(16) NOT NULL,
 next_spawn_at BIGINT NOT NULL DEFAULT 0,eta_kind VARCHAR(24) NOT NULL,
 version BIGINT NOT NULL,updated_at BIGINT NOT NULL,
 KEY appeared_idx(appeared_at),KEY history_idx(updated_at),KEY boss_idx(boss_id,appeared_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
