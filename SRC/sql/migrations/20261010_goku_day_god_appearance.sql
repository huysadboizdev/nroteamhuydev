-- Keep the genuine red Goku head; use the complete existing Goku uniform animation.
-- Do not replace reindeer artwork shared by other templates.
DELIMITER $$
DROP PROCEDURE IF EXISTS repair_goku_day_god_20261010$$
CREATE PROCEDURE repair_goku_day_god_20261010()
BEGIN
    IF (SELECT COUNT(*) FROM item_template WHERE id=1593 AND TYPE=5 AND head=1467)<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected Goku SSJ God template';
    END IF;
    IF (SELECT COUNT(*) FROM part WHERE
        (id=1467 AND TYPE=0 AND JSON_VALID(DATA) AND JSON_LENGTH(DATA)=3) OR
        (id=1450 AND TYPE=1 AND JSON_VALID(DATA) AND JSON_LENGTH(DATA)=17) OR
        (id=1451 AND TYPE=2 AND JSON_VALID(DATA) AND JSON_LENGTH(DATA)=14))<>3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid genuine Goku parts';
    END IF;
    UPDATE item_template SET body=1450,leg=1451 WHERE id=1593;
END$$
DELIMITER ;
START TRANSACTION;
CALL repair_goku_day_god_20261010();
COMMIT;
DROP PROCEDURE repair_goku_day_god_20261010;
