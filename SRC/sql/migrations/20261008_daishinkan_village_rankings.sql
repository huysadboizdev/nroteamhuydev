-- Genuine Grand Priest assets: head703/body704/leg705, portrait6578.
-- Preserve every other map NPC, including Bo Mong outside houses21..23.
-- Repeated application normalizes exactly one Daishinkan per starter village.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_daishinkan_village_rankings$$
CREATE PROCEDURE migrate_daishinkan_village_rankings()
BEGIN
    DECLARE map_number INT;
    DECLARE row_index INT;
    DECLARE map_npcs LONGTEXT;
    DECLARE npc_x INT;
    DECLARE npc_y INT;
    DECLARE target_map INT;
    IF EXISTS (SELECT 1 FROM npc_template WHERE id=111 AND NAME<>'Daishinkan') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='NPC111 already belongs to another NPC';
    END IF;
    IF (SELECT COUNT(*) FROM part WHERE id IN (703,704,705))<>3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing original Daishinkan parts';
    END IF;
    IF (SELECT COUNT(*) FROM map_template WHERE id IN (0,7,14,21,22,23) AND JSON_VALID(npcs))<>6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid starter map NPC data';
    END IF;
    INSERT INTO npc_template (id,NAME,head,body,leg,avatar)
        VALUES (111,'Daishinkan',703,704,705,6578)
        ON DUPLICATE KEY UPDATE head=703,body=704,leg=705,avatar=6578;
    SET map_number=0;
    WHILE map_number<6 DO
        SET map_number=map_number+1;
        SET target_map=CASE map_number WHEN 1 THEN 0 WHEN 2 THEN 7 WHEN 3 THEN 14 WHEN 4 THEN 21 WHEN 5 THEN 22 ELSE 23 END;
        SELECT npcs INTO map_npcs FROM map_template WHERE id=target_map;
        SET row_index=JSON_LENGTH(map_npcs)-1;
        WHILE row_index>=0 DO
            IF (target_map IN (21,22,23) AND JSON_EXTRACT(map_npcs,CONCAT('$[',row_index,'][0]'))=17)
                OR (target_map IN (0,7,14) AND JSON_EXTRACT(map_npcs,CONCAT('$[',row_index,'][0]'))=111) THEN
                SET map_npcs=JSON_REMOVE(map_npcs,CONCAT('$[',row_index,']'));
            END IF;
            SET row_index=row_index-1;
        END WHILE;
        IF target_map IN (0,7,14) THEN
            SET npc_x=1100;
            SET npc_y=CASE target_map WHEN 14 THEN 408 ELSE 432 END;
            SET map_npcs=JSON_ARRAY_APPEND(map_npcs,'$',JSON_ARRAY(111,CAST(npc_x AS SIGNED),CAST(npc_y AS SIGNED)));
        END IF;
        UPDATE map_template SET npcs=map_npcs WHERE id=target_map;
    END WHILE;
END$$
DELIMITER ;
START TRANSACTION;
CALL migrate_daishinkan_village_rankings();
COMMIT;
DROP PROCEDURE migrate_daishinkan_village_rankings;
