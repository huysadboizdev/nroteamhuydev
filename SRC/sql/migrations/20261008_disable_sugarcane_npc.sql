-- Disable Xe nuoc mia NPC84 placements; preserve every other NPC/template.
DELIMITER $$
DROP PROCEDURE IF EXISTS disable_sugarcane_npc_20261008$$
CREATE PROCEDURE disable_sugarcane_npc_20261008()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE target_map INT;
    DECLARE map_npcs LONGTEXT;
    DECLARE row_index INT;
    DECLARE map_cursor CURSOR FOR SELECT id,npcs FROM map_template WHERE JSON_VALID(npcs);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
    OPEN map_cursor;
    map_loop: LOOP
        FETCH map_cursor INTO target_map,map_npcs;
        IF done=1 THEN LEAVE map_loop; END IF;
        SET row_index=JSON_LENGTH(map_npcs)-1;
        WHILE row_index>=0 DO
            IF JSON_EXTRACT(map_npcs,CONCAT('$[',row_index,'][0]'))=84 THEN
                SET map_npcs=JSON_REMOVE(map_npcs,CONCAT('$[',row_index,']'));
            END IF;
            SET row_index=row_index-1;
        END WHILE;
        UPDATE map_template SET npcs=map_npcs WHERE id=target_map;
    END LOOP;
    CLOSE map_cursor;
END$$
DELIMITER ;
START TRANSACTION;
CALL disable_sugarcane_npc_20261008();
COMMIT;
DROP PROCEDURE disable_sugarcane_npc_20261008;
