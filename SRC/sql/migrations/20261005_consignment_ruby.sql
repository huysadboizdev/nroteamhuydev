-- CONSIGNMENT_RUBY_ONLY_20261005
-- Run with the application stopped. Existing listing ids and prices stay unchanged.
-- Historical rows remain legacy; only newly posted listings use ruby.
SET @consign_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shop_ky_gui' AND column_name='currency'), 'SELECT 1', 'ALTER TABLE shop_ky_gui ADD COLUMN currency VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT ''legacy''');
PREPARE consign_stmt FROM @consign_sql;
EXECUTE consign_stmt;
DEALLOCATE PREPARE consign_stmt;
SET @consign_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shop_ky_gui' AND column_name='create_time'), 'SELECT 1', 'ALTER TABLE shop_ky_gui ADD COLUMN create_time BIGINT NOT NULL DEFAULT 0');
PREPARE consign_stmt FROM @consign_sql;
EXECUTE consign_stmt;
DEALLOCATE PREPARE consign_stmt;
SET @consign_sql = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='shop_ky_gui' AND index_name='PRIMARY'), 'SELECT 1', 'ALTER TABLE shop_ky_gui ADD PRIMARY KEY(id)');
PREPARE consign_stmt FROM @consign_sql;
EXECUTE consign_stmt;
DEALLOCATE PREPARE consign_stmt;
SET @consign_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='shop_ky_gui' AND column_name='id' AND extra LIKE '%auto_increment%'), 'SELECT 1', 'ALTER TABLE shop_ky_gui MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT');
PREPARE consign_stmt FROM @consign_sql;
EXECUTE consign_stmt;
DEALLOCATE PREPARE consign_stmt;
SET @consign_sql = IF(EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='shop_ky_gui' AND engine='InnoDB'), 'SELECT 1', 'ALTER TABLE shop_ky_gui ENGINE=InnoDB');
PREPARE consign_stmt FROM @consign_sql;
EXECUTE consign_stmt;
DEALLOCATE PREPARE consign_stmt;
SET @consign_sql = IF(EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='player' AND engine='InnoDB'), 'SELECT 1', 'ALTER TABLE player ENGINE=InnoDB');
PREPARE consign_stmt FROM @consign_sql;
EXECUTE consign_stmt;
DEALLOCATE PREPARE consign_stmt;
CREATE TABLE IF NOT EXISTS consign_receipt (
 request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 action VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 player_id BIGINT NOT NULL,
 account_id INT NOT NULL,
 listing_id INT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY player_created(player_id,created_at),
 KEY listing_history(listing_id,created_at)
) ENGINE=InnoDB;
