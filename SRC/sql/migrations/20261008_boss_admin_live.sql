-- Additive schema; no alteration/removal of legacy overrides or player data.
CREATE TABLE IF NOT EXISTS boss_admin_state (
 id TINYINT NOT NULL PRIMARY KEY,
 revision BIGINT NOT NULL DEFAULT 0,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS boss_admin_runtime (
 boss_key VARCHAR(160) COLLATE utf8mb4_bin NOT NULL,
 phase INT NOT NULL,
 alive_count INT NOT NULL,
 current_hp INT NOT NULL DEFAULT 0,
 max_hp INT NOT NULL DEFAULT 0,
 damage INT NOT NULL DEFAULT 0,
 applied_revision BIGINT NOT NULL,
 server_run VARCHAR(40) NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(boss_key,phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO boss_admin_state(id,revision) VALUES(1,0);
CREATE TABLE IF NOT EXISTS boss_admin_config (
 boss_key VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 phase INT NOT NULL DEFAULT 0,
 hp INT NULL,
 damage INT NULL,
 cap_damage INT NULL,
 cap_percent DECIMAL(7,3) NULL,
 apply_mode VARCHAR(16) NOT NULL DEFAULT 'next',
 drop_mode VARCHAR(16) NOT NULL DEFAULT 'default',
 drops LONGTEXT NOT NULL DEFAULT '[]',
 revision BIGINT NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(boss_key,phase),
 CONSTRAINT boss_admin_phase CHECK(phase BETWEEN 0 AND 31),
 CONSTRAINT boss_admin_hp CHECK(hp IS NULL OR hp>0),
 CONSTRAINT boss_admin_damage CHECK(damage IS NULL OR damage>=0),
 CONSTRAINT boss_admin_cap CHECK(cap_damage IS NULL OR cap_damage>=0),
 CONSTRAINT boss_admin_percent CHECK(cap_percent IS NULL OR cap_percent BETWEEN 0 AND 100),
 CONSTRAINT boss_admin_modes CHECK(apply_mode IN('next','alive') AND drop_mode IN('default','replace')),
 CONSTRAINT boss_admin_drops CHECK(JSON_VALID(drops) AND JSON_TYPE(drops)='ARRAY'),
 CONSTRAINT boss_admin_revision CHECK(revision>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS boss_admin_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 boss_key VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 phase INT NOT NULL,
 revision BIGINT NOT NULL,
 actor_id INT NOT NULL,
 before_json LONGTEXT NULL,
 after_json LONGTEXT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY boss_admin_history(boss_key,phase,id),
 UNIQUE KEY boss_admin_audit_revision(revision),
 CONSTRAINT boss_admin_before CHECK(before_json IS NULL OR JSON_VALID(before_json)),
 CONSTRAINT boss_admin_after CHECK(JSON_VALID(after_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
