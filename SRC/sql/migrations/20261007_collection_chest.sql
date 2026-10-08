-- COLLECTION_CHEST_20261007
-- Apply after data-saving maintenance and SQL backup.
SET NAMES utf8mb4;
SET @has_collection=(SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='player' AND COLUMN_NAME='items_collection');
SET @collection_ddl=IF(@has_collection=0,
    'ALTER TABLE player ADD COLUMN items_collection MEDIUMTEXT NOT NULL DEFAULT ''[]''',
    'SELECT 1');
PREPARE collection_stmt FROM @collection_ddl;
EXECUTE collection_stmt;
DEALLOCATE PREPARE collection_stmt;
