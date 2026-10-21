SET NAMES utf8mb4;
DELIMITER //
DROP PROCEDURE IF EXISTS apply_clan_pet_mount_ruby_shop_20261021//
CREATE PROCEDURE apply_clan_pet_mount_ruby_shop_20261021()
BEGIN
    DECLARE santa_shop_id INT;
    DECLARE clan_shop_id INT;
    DECLARE pet_tab_id INT;
    DECLARE ride_tab_id INT;
    DECLARE source_count INT DEFAULT 0;
    DECLARE template_count INT DEFAULT 0;
    DECLARE next_item_shop_id INT DEFAULT 0;
    DECLARE source_item_shop_id INT;
    DECLARE source_tab_id INT;
    DECLARE item_temp_id INT;
    DECLARE target_tab_id INT;
    DECLARE target_item_shop_id INT;
    DECLARE done INT DEFAULT 0;
    DECLARE item_cursor CURSOR FOR
        SELECT i.id, i.tab_id, i.temp_id
        FROM item_shop i
        JOIN tab_shop t ON t.id=i.tab_id
        JOIN shop s ON s.id=t.shop_id
        WHERE s.id=clan_shop_id AND t.id IN (61,62)
        ORDER BY i.tab_id, i.temp_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*), MIN(id) INTO source_count, clan_shop_id
    FROM shop WHERE tag_name='SHOP_CLAN';
    IF source_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly one SHOP_CLAN shop';
    END IF;
    SELECT COUNT(*), MIN(id) INTO source_count, santa_shop_id
    FROM shop WHERE tag_name='SANTA_HONG_NGOC' AND npc_id=39 AND type_shop=3;
    IF source_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly one Santa ruby shop';
    END IF;
    SELECT COUNT(*) INTO source_count
    FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id
    WHERE t.shop_id=clan_shop_id AND t.id IN (61,62);
    IF source_count <> 21 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected 21 Pet and Ván Bay shop rows';
    END IF;
    SELECT COUNT(DISTINCT it.id) INTO template_count
    FROM item_template it JOIN item_shop i ON i.temp_id=it.id
    JOIN tab_shop t ON t.id=i.tab_id
    WHERE t.shop_id=clan_shop_id AND t.id IN (61,62);
    IF template_count <> 21 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='One or more Pet/Ván Bay item templates are missing';
    END IF;

    START TRANSACTION;
    SELECT MIN(id) INTO pet_tab_id FROM tab_shop WHERE shop_id=santa_shop_id AND NAME='Pet';
    IF pet_tab_id IS NULL THEN
        SELECT COALESCE(MAX(id),0)+1 INTO pet_tab_id FROM tab_shop;
        INSERT INTO tab_shop(id,shop_id,NAME) VALUES(pet_tab_id,santa_shop_id,'Pet');
    END IF;
    SELECT MIN(id) INTO ride_tab_id FROM tab_shop WHERE shop_id=santa_shop_id AND NAME='Ván Bay';
    IF ride_tab_id IS NULL THEN
        SELECT COALESCE(MAX(id),0)+1 INTO ride_tab_id FROM tab_shop;
        INSERT INTO tab_shop(id,shop_id,NAME) VALUES(ride_tab_id,santa_shop_id,'Ván Bay');
    END IF;

    SELECT COALESCE(MAX(id),0) INTO next_item_shop_id FROM item_shop;
    OPEN item_cursor;
    copy_items: LOOP
        FETCH item_cursor INTO source_item_shop_id, source_tab_id, item_temp_id;
        IF done=1 THEN LEAVE copy_items; END IF;
        SET target_tab_id=IF(source_tab_id=61,pet_tab_id,ride_tab_id);
        SELECT MIN(id) INTO target_item_shop_id
        FROM item_shop WHERE tab_id=target_tab_id AND temp_id=item_temp_id;
        IF target_item_shop_id IS NULL THEN
            SET next_item_shop_id=next_item_shop_id+1;
            SET target_item_shop_id=next_item_shop_id;
            INSERT INTO item_shop(id,tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,create_time)
            SELECT target_item_shop_id,target_tab_id,temp_id,1,1,3,50,7743,NOW()
            FROM item_shop WHERE id=source_item_shop_id;
        ELSE
            UPDATE item_shop SET is_new=1,is_sell=1,type_sell=3,cost=50,icon_spec=7743
            WHERE id=target_item_shop_id;
        END IF;
    END LOOP;
    CLOSE item_cursor;

    SELECT COALESCE(MAX(id),0) INTO @next_clan_ruby_option_id FROM item_shop_option;
    INSERT INTO item_shop_option(id,item_shop_id,option_id,param)
    SELECT (@next_clan_ruby_option_id:=@next_clan_ruby_option_id+1), target.id, source_option.option_id, source_option.param
    FROM item_shop source
    JOIN item_shop_option source_option ON source_option.item_shop_id=source.id
    JOIN tab_shop source_tab ON source_tab.id=source.tab_id
    JOIN item_shop target ON target.tab_id=IF(source.tab_id=61,pet_tab_id,ride_tab_id)
        AND target.temp_id=source.temp_id
    WHERE source_tab.shop_id=clan_shop_id AND source.tab_id IN (61,62)
      AND NOT EXISTS (
          SELECT 1 FROM item_shop_option existing_option
          WHERE existing_option.item_shop_id=target.id
            AND existing_option.option_id=source_option.option_id
            AND existing_option.param=source_option.param
      )
    ORDER BY source.tab_id, source.temp_id, source_option.id;

    UPDATE item_shop i
    JOIN tab_shop t ON t.id=i.tab_id
    SET i.is_sell=0
    WHERE t.shop_id=clan_shop_id AND t.id IN (60,61,62);
    COMMIT;
END//
DELIMITER ;
CALL apply_clan_pet_mount_ruby_shop_20261021();
DROP PROCEDURE apply_clan_pet_mount_ruby_shop_20261021;
