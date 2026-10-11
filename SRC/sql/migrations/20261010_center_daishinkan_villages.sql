-- Center only Daishinkan between existing village anchors; preserve every other NPC.
DELIMITER $$
DROP PROCEDURE IF EXISTS center_daishinkan_villages_20261010$$
CREATE PROCEDURE center_daishinkan_villages_20261010()
BEGIN
    DECLARE village INT;
    DECLARE map_id INT;
    DECLARE anchor_id INT;
    DECLARE entries LONGTEXT;
    DECLARE idx INT;
    DECLARE npc_id INT;
    DECLARE daishinkan_idx INT;
    DECLARE daishinkan_count INT;
    DECLARE tori_count INT;
    DECLARE anchor_count INT;
    DECLARE tori_x INT;
    DECLARE anchor_x INT;
    -- Preflight every map before changing any map.
    SET village=0;
    WHILE village<6 DO
        SET map_id=CASE MOD(village,3) WHEN 0 THEN 0 WHEN 1 THEN 7 ELSE 14 END;
        SET anchor_id=CASE map_id WHEN 0 THEN 67 WHEN 7 THEN 8 ELSE 9 END;
        SET entries=NULL;
        SELECT npcs INTO entries FROM map_template WHERE id=map_id;
        IF entries IS NULL OR NOT JSON_VALID(entries) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid village NPC data';
        END IF;
        SET idx=0;
        SET daishinkan_count=0;
        SET tori_count=0;
        SET anchor_count=0;
        WHILE idx<JSON_LENGTH(entries) DO
            SET npc_id=JSON_EXTRACT(entries,CONCAT('$[',idx,'][0]'));
            IF npc_id=111 THEN
                SET daishinkan_idx=idx;
                SET daishinkan_count=daishinkan_count+1;
            ELSEIF npc_id=74 THEN
                SET tori_count=tori_count+1;
                SET tori_x=JSON_EXTRACT(entries,CONCAT('$[',idx,'][1]'));
            ELSEIF npc_id=anchor_id THEN
                SET anchor_count=anchor_count+1;
                SET anchor_x=JSON_EXTRACT(entries,CONCAT('$[',idx,'][1]'));
            END IF;
            SET idx=idx+1;
        END WHILE;
        IF daishinkan_count<>1 OR tori_count<>1 OR anchor_count<>1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected unique Daishinkan and village anchors';
        END IF;
        IF village>=3 THEN
            SET entries=JSON_SET(entries,
                CONCAT('$[',daishinkan_idx,'][1]'),CAST(ROUND((tori_x+anchor_x)/2) AS SIGNED),
                CONCAT('$[',daishinkan_idx,'][2]'),CAST(IF(map_id=14,408,432) AS SIGNED));
            UPDATE map_template SET npcs=entries WHERE id=map_id;
        END IF;
        SET village=village+1;
    END WHILE;
END$$
DELIMITER ;
START TRANSACTION;
CALL center_daishinkan_villages_20261010();
COMMIT;
DROP PROCEDURE center_daishinkan_villages_20261010;
