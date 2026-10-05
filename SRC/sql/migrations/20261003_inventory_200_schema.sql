-- Apply while the game server is stopped, after backing up schema and data.
-- Slot counts are JSON array lengths, not separate SQL columns.
ALTER TABLE player
  MODIFY COLUMN items_bag MEDIUMTEXT NOT NULL,
  MODIFY COLUMN items_box MEDIUMTEXT NOT NULL;
