-- Ngũ Hành Sơn: village guides, three connected populated maps and final exchange NPCs.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS codex_setup_ngu_hanh_son;
DELIMITER $$
CREATE PROCEDURE codex_setup_ngu_hanh_son()
BEGIN
    DECLARE mid INT;
    DECLARE idx INT;
    DECLARE old_npcs LONGTEXT;
    DECLARE new_npcs LONGTEXT;
    DECLARE entry LONGTEXT;
    DECLARE npc_id INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
    IF (SELECT COUNT(*) FROM map_template WHERE id IN(0,7,14,122,123,124))<>6
       OR (SELECT COUNT(*) FROM npc_template WHERE id IN(48,49))<>2
       OR (SELECT COUNT(*) FROM item_template WHERE id IN(544,545,546,547,548,1698) AND type=5)<>6
       OR (SELECT COUNT(*) FROM item_template WHERE id IN(541,542))<>2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing Tay Du maps/NPCs/items; setup cancelled';
    END IF;
    IF EXISTS(SELECT 1 FROM map_template WHERE id IN(0,7,14,122,123,124) AND JSON_VALID(npcs)=0)
       OR EXISTS(SELECT 1 FROM map_template WHERE id IN(122,123,124) AND (JSON_VALID(mobs)=0 OR JSON_LENGTH(mobs)=0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid NPCs or empty monster map; setup cancelled';
    END IF;
    IF (SELECT COUNT(*) FROM part WHERE
        (id IN(457,460,461,462,465,467,1583) AND type=0 AND JSON_VALID(data) AND JSON_LENGTH(data)=3)
        OR (id IN(458,463,466,468,470,1584) AND type=1 AND JSON_VALID(data) AND JSON_LENGTH(data)=17)
        OR (id IN(459,464,469,471,1585) AND type=2 AND JSON_VALID(data) AND JSON_LENGTH(data)=14))<>18 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or invalid costume/NPC parts; setup cancelled';
    END IF;
    SET mid=0;
    WHILE mid<6 DO
        SET @ngu_map=ELT(mid+1,0,7,14,122,123,124);
        SELECT npcs INTO old_npcs FROM map_template WHERE id=@ngu_map;
        SET new_npcs=JSON_ARRAY(); SET idx=0;
        WHILE idx<JSON_LENGTH(old_npcs) DO
            SET entry=JSON_EXTRACT(old_npcs,CONCAT('$[',idx,']'));
            IF JSON_TYPE(entry)='STRING' THEN
                SET entry=JSON_UNQUOTE(entry);
            END IF;
            IF JSON_VALID(entry)=0 OR JSON_TYPE(entry)<>'ARRAY' OR JSON_LENGTH(entry)<3 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid NPC entry; setup cancelled';
            END IF;
            SET npc_id=CAST(JSON_UNQUOTE(JSON_EXTRACT(entry,'$[0]')) AS SIGNED);
            IF npc_id<>49 AND (@ngu_map NOT IN(122,123,124) OR npc_id<>48) THEN
                SET new_npcs=JSON_ARRAY_APPEND(new_npcs,'$',JSON_EXTRACT(entry,'$'));
            END IF;
            SET idx=idx+1;
        END WHILE;
        IF @ngu_map=0 THEN SET new_npcs=JSON_ARRAY_APPEND(new_npcs,'$',JSON_ARRAY(49,672,432));
        ELSEIF @ngu_map=7 THEN SET new_npcs=JSON_ARRAY_APPEND(new_npcs,'$',JSON_ARRAY(49,689,432));
        ELSEIF @ngu_map=14 THEN SET new_npcs=JSON_ARRAY_APPEND(new_npcs,'$',JSON_ARRAY(49,478,408));
        ELSEIF @ngu_map=124 THEN
            SET new_npcs=JSON_ARRAY_APPEND(new_npcs,'$',JSON_ARRAY(49,960,288),'$',JSON_ARRAY(48,1080,288));
        END IF;
        UPDATE map_template SET npcs=new_npcs WHERE id=@ngu_map;
        SET mid=mid+1;
    END WHILE;
    UPDATE map_template SET name='Ngũ Hành Sơn 1',waypoints=JSON_ARRAY(
        JSON_ARRAY('Ngũ Hành Sơn 2',1368,384,1440,456,0,0,123,60,384)) WHERE id=122;
    UPDATE map_template SET name='Ngũ Hành Sơn 2',waypoints=JSON_ARRAY(
        JSON_ARRAY('Ngũ Hành Sơn 1',0,336,48,408,0,0,122,1380,432),
        JSON_ARRAY('Ngũ Hành Sơn 3',1464,336,1536,384,0,0,124,60,360)) WHERE id=123;
    UPDATE map_template SET name='Ngũ Hành Sơn 3',waypoints=JSON_ARRAY(
        JSON_ARRAY('Ngũ Hành Sơn 2',0,336,48,384,0,0,123,1476,360)) WHERE id=124;
    UPDATE npc_template SET name='Tôn Ngộ Không',avatar=4468 WHERE id=48;
    COMMIT;
END$$
DELIMITER ;
CALL codex_setup_ngu_hanh_son();
DROP PROCEDURE codex_setup_ngu_hanh_son;
