-- Apply only during saved maintenance, after a backup, before installing the gift transaction JAR.
-- No catalog foreign key: deleting/recreating a code or player must not erase account claims.
DELIMITER $$
DROP FUNCTION IF EXISTS giftcode_canonical_20261007$$
CREATE FUNCTION giftcode_canonical_20261007(raw TEXT) RETURNS VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin DETERMINISTIC
BEGIN
 DECLARE value TEXT;
 DECLARE i INT DEFAULT 1;
 IF raw IS NULL THEN RETURN NULL; END IF;
 SET value=raw;
 WHILE LENGTH(value)>0 AND ASCII(LEFT(value,1)) IN (9,10,11,12,13,32) DO SET value=SUBSTRING(value,2); END WHILE;
 WHILE LENGTH(value)>0 AND ASCII(RIGHT(value,1)) IN (9,10,11,12,13,32) DO SET value=LEFT(value,CHAR_LENGTH(value)-1); END WHILE;
 IF CHAR_LENGTH(value)=0 OR CHAR_LENGTH(value)>100 THEN RETURN NULL; END IF;
 WHILE i<=CHAR_LENGTH(value) DO
  IF ORD(SUBSTRING(value,i,1))<33 OR ORD(SUBSTRING(value,i,1))>126 THEN RETURN NULL; END IF;
  SET i=i+1;
 END WHILE;
 RETURN UPPER(value);
END$$
DROP PROCEDURE IF EXISTS migrate_giftcode_20261007$$
CREATE PROCEDURE migrate_giftcode_20261007()
BEGIN
 DECLARE done INT DEFAULT 0;
 DECLARE account_value INT;
 DECLARE player_value BIGINT;
 DECLARE history_value LONGTEXT;
 DECLARE index_value INT;
 DECLARE raw_value TEXT;
 DECLARE code_value VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin;
 DECLARE histories CURSOR FOR SELECT id,account_id,giftcode FROM player ORDER BY id;
 DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
 -- Preflight all data into temporary staging BEFORE any persistent DDL/data change.
 IF EXISTS(SELECT 1 FROM giftcode WHERE giftcode_canonical_20261007(code) IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid legacy giftcode: fix empty/space/non-ASCII/overlong codes first';
 END IF;
 IF EXISTS(SELECT 1 FROM giftcode GROUP BY giftcode_canonical_20261007(code) HAVING COUNT(*)>1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Canonical giftcode duplicates: merge manually before migration';
 END IF;
 DROP TEMPORARY TABLE IF EXISTS giftcode_claim_backfill_20261007;
 CREATE TEMPORARY TABLE giftcode_claim_backfill_20261007(account_id INT NOT NULL,canonical_code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,player_id BIGINT NOT NULL,PRIMARY KEY(account_id,canonical_code)) ENGINE=InnoDB;
 OPEN histories;
 scan_loop: LOOP
  FETCH histories INTO player_value,account_value,history_value;
  IF done=1 THEN LEAVE scan_loop; END IF;
  -- Existing player creation writes empty text for no claims; accept it as an empty array.
  IF history_value='' THEN SET history_value='[]'; END IF;
  IF account_value<=0 OR JSON_VALID(history_value)=0 THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid player account or legacy giftcode JSON: migration refused';
  END IF;
  IF JSON_TYPE(history_value)<>'ARRAY' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Player giftcode history must be JSON array'; END IF;
  SET index_value=0;
  WHILE index_value<JSON_LENGTH(history_value) DO
   IF JSON_TYPE(JSON_EXTRACT(history_value,CONCAT('$[',index_value,']')))<>'STRING' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid non-string legacy giftcode history'; END IF;
   SET raw_value=JSON_UNQUOTE(JSON_EXTRACT(history_value,CONCAT('$[',index_value,']')));
   SET code_value=giftcode_canonical_20261007(raw_value);
   IF code_value IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid legacy claimed code: migration refused'; END IF;
   INSERT IGNORE INTO giftcode_claim_backfill_20261007 VALUES(account_value,code_value,player_value);
   SET index_value=index_value+1;
  END WHILE;
 END LOOP;
 CLOSE histories;
 CREATE TABLE IF NOT EXISTS giftcode_claim(
  account_id INT NOT NULL,
  canonical_code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  player_id BIGINT NOT NULL,
  catalog_id INT NOT NULL DEFAULT 0,
  remaining_after INT NOT NULL DEFAULT 0,
  claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(account_id,canonical_code), UNIQUE KEY request_id(request_id)
 ) ENGINE=InnoDB;
 -- Backfill applies even when the original catalog code no longer exists.
 INSERT IGNORE INTO giftcode_claim(account_id,canonical_code,request_id,player_id)
  SELECT account_id,canonical_code,UUID(),player_id FROM giftcode_claim_backfill_20261007;
 -- Remove ON UPDATE before canonicalization, retaining original creation timestamps.
 ALTER TABLE giftcode MODIFY datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
 UPDATE giftcode SET code=giftcode_canonical_20261007(code);
 ALTER TABLE giftcode MODIFY code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='giftcode' AND index_name='giftcode_canonical_unique') THEN
  ALTER TABLE giftcode ADD UNIQUE KEY giftcode_canonical_unique(code);
 END IF;
 DROP TEMPORARY TABLE giftcode_claim_backfill_20261007;
END$$
CALL migrate_giftcode_20261007()$$
DROP PROCEDURE migrate_giftcode_20261007$$
DROP FUNCTION giftcode_canonical_20261007$$
DELIMITER ;
