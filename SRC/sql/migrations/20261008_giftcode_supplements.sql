-- Apply during saved maintenance after 20261007_giftcode_claims.sql and a backup.
-- Baselines are captured exactly once. NULL means unknown historical delivery and
-- must remain NULL even if an administrator later recreates the catalog code.
DELIMITER $$
DROP FUNCTION IF EXISTS giftcode_baseline_20261008$$
CREATE FUNCTION giftcode_baseline_20261008(raw LONGTEXT) RETURNS LONGTEXT DETERMINISTIC
BEGIN
 DECLARE i INT DEFAULT 0;
 DECLARE j INT DEFAULT 0;
 DECLARE k INT DEFAULT 0;
 DECLARE entry_value LONGTEXT;
 DECLARE options_value LONGTEXT;
 DECLARE option_value LONGTEXT;
 DECLARE normalized LONGTEXT DEFAULT '[]';
 DECLARE id_value BIGINT;
 DECLARE quantity_value BIGINT;
 DECLARE option_id BIGINT;
 DECLARE param_value BIGINT;
 IF raw IS NULL OR JSON_VALID(raw)=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid giftcode baseline JSON'; END IF;
 IF JSON_TYPE(raw)<>'ARRAY' OR JSON_LENGTH(raw)>200 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Giftcode baseline must be array of at most 200 rewards'; END IF;
 WHILE i<JSON_LENGTH(raw) DO
  SET entry_value=JSON_EXTRACT(raw,CONCAT('$[',i,']'));
  IF JSON_TYPE(entry_value)<>'OBJECT' OR COALESCE(JSON_TYPE(JSON_EXTRACT(entry_value,'$.id')),'NULL')<>'INTEGER' OR COALESCE(JSON_TYPE(JSON_EXTRACT(entry_value,'$.quantity')),'NULL')<>'INTEGER' THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Giftcode baseline reward needs integer id and quantity';
  END IF;
  -- Check lexical magnitude before BIGINT assignment to avoid overflow/coercion.
  IF JSON_EXTRACT(entry_value,'$.id') < -3 OR JSON_EXTRACT(entry_value,'$.id') > 32767 OR JSON_EXTRACT(entry_value,'$.quantity') < 1 OR JSON_EXTRACT(entry_value,'$.quantity') > 2147483647 THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Giftcode baseline reward outside supported range';
  END IF;
  SET id_value=JSON_EXTRACT(entry_value,'$.id');
  SET quantity_value=JSON_EXTRACT(entry_value,'$.quantity');
  SET k=0;
  WHILE k<i DO
   IF JSON_EXTRACT(raw,CONCAT('$[',k,'].id'))=id_value THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate giftcode baseline reward id'; END IF;
   SET k=k+1;
  END WHILE;
  IF JSON_CONTAINS_PATH(entry_value,'one','$.options')=0 THEN SET options_value='[]';
  ELSE SET options_value=JSON_EXTRACT(entry_value,'$.options'); END IF;
  IF JSON_TYPE(options_value)<>'ARRAY' OR JSON_LENGTH(options_value)>100 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Giftcode baseline options must be array of at most 100 options'; END IF;
  SET j=0;
  WHILE j<JSON_LENGTH(options_value) DO
   SET option_value=JSON_EXTRACT(options_value,CONCAT('$[',j,']'));
   IF JSON_TYPE(option_value)<>'OBJECT' OR COALESCE(JSON_TYPE(JSON_EXTRACT(option_value,'$.id')),'NULL')<>'INTEGER' OR COALESCE(JSON_TYPE(JSON_EXTRACT(option_value,'$.param')),'NULL')<>'INTEGER' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Giftcode baseline option needs integer id and param';
   END IF;
   IF JSON_EXTRACT(option_value,'$.id')<0 OR JSON_EXTRACT(option_value,'$.id')>32767 OR JSON_EXTRACT(option_value,'$.param')<0 OR JSON_EXTRACT(option_value,'$.param')>2147483647 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Giftcode baseline option outside supported range';
   END IF;
   SET option_id=JSON_EXTRACT(option_value,'$.id');
   SET param_value=JSON_EXTRACT(option_value,'$.param');
   SET k=0;
   WHILE k<j DO
    IF JSON_EXTRACT(options_value,CONCAT('$[',k,'].id'))=option_id THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate giftcode baseline option id'; END IF;
    SET k=k+1;
   END WHILE;
   SET j=j+1;
  END WHILE;
  SET normalized=JSON_ARRAY_APPEND(normalized,'$',JSON_OBJECT('id',CAST(id_value AS SIGNED),'quantity',CAST(quantity_value AS SIGNED),'options',JSON_EXTRACT(options_value,'$')));
  SET i=i+1;
 END WHILE;
 RETURN normalized;
END$$
DROP PROCEDURE IF EXISTS migrate_giftcode_20261008$$
CREATE PROCEDURE migrate_giftcode_20261008()
BEGIN
 DECLARE already_applied INT DEFAULT 0;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='giftcode_schema_migration') THEN
  SELECT COUNT(*) INTO already_applied FROM giftcode_schema_migration WHERE migration_id='20261008_giftcode_supplements';
 END IF;
 DROP TEMPORARY TABLE IF EXISTS giftcode_baseline_stage_20261008;
 CREATE TEMPORARY TABLE giftcode_baseline_stage_20261008(
  account_id INT NOT NULL,
  canonical_code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  delivered_detail LONGTEXT NOT NULL,
  PRIMARY KEY(account_id,canonical_code)
 ) ENGINE=InnoDB;
 -- JSON validation/staging happens before any persistent table/column mutation.
 -- Marker reruns deliberately do not inspect today's catalog or infer new baselines.
 IF already_applied=0 THEN
  INSERT INTO giftcode_baseline_stage_20261008(account_id,canonical_code,delivered_detail)
   SELECT c.account_id,c.canonical_code,giftcode_baseline_20261008(g.detail)
   FROM giftcode_claim c INNER JOIN giftcode g ON g.code=c.canonical_code;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='giftcode_claim' AND column_name='delivered_detail') THEN
  ALTER TABLE giftcode_claim ADD COLUMN delivered_detail LONGTEXT NULL;
 END IF;
 -- Web catalog edits lock only the account watermarks for the edited code.
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='giftcode_claim' AND index_name='giftcode_claim_code') THEN
  ALTER TABLE giftcode_claim ADD KEY giftcode_claim_code(canonical_code,account_id);
 END IF;
 CREATE TABLE IF NOT EXISTS giftcode_claim_request(
  request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  account_id INT NOT NULL,
  canonical_code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  player_id BIGINT NOT NULL,
  catalog_id INT NOT NULL,
  remaining_after INT NOT NULL,
  claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(request_id),
  KEY giftcode_request_account_code(account_id,canonical_code)
 ) ENGINE=InnoDB;
 CREATE TABLE IF NOT EXISTS giftcode_schema_migration(
  migration_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(migration_id)
 ) ENGINE=InnoDB;
 START TRANSACTION;
 IF already_applied=0 THEN
  UPDATE giftcode_claim c INNER JOIN giftcode_baseline_stage_20261008 b
   ON b.account_id=c.account_id AND b.canonical_code=c.canonical_code
   SET c.delivered_detail=b.delivered_detail;
  INSERT INTO giftcode_claim_request(request_id,account_id,canonical_code,player_id,catalog_id,remaining_after,claimed_at)
   SELECT request_id,account_id,canonical_code,player_id,catalog_id,remaining_after,claimed_at FROM giftcode_claim;
  INSERT INTO giftcode_schema_migration(migration_id) VALUES('20261008_giftcode_supplements');
 END IF;
 COMMIT;
 DROP TEMPORARY TABLE giftcode_baseline_stage_20261008;
END$$
CALL migrate_giftcode_20261008()$$
DROP PROCEDURE migrate_giftcode_20261008$$
DROP FUNCTION giftcode_baseline_20261008$$
DELIMITER ;
