-- Boss Admin extra stats: armor (defense) and crit (critical), nullable = game default.
-- Additive only. Single-apply with the deploy script (MySQL 8 has no IF NOT EXISTS for columns).
ALTER TABLE boss_admin_config
  ADD COLUMN defense INT NULL,
  ADD COLUMN critical INT NULL,
  ADD CONSTRAINT boss_admin_defense CHECK (defense IS NULL OR defense >= 0),
  ADD CONSTRAINT boss_admin_critical CHECK (critical IS NULL OR critical BETWEEN 0 AND 100);
