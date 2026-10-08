-- Sparse overrides; missing rows retain compiled defaults and scheduling.
CREATE TABLE IF NOT EXISTS boss_live_override (
 boss_id INT NOT NULL,
 phase INT NOT NULL DEFAULT 0,
 hp INT NULL,
 damage INT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(boss_id,phase),
 CHECK (phase BETWEEN 0 AND 31),
 CHECK (hp IS NULL OR hp > 0),
 CHECK (damage IS NULL OR damage >= 0),
 CHECK (hp IS NOT NULL OR damage IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
