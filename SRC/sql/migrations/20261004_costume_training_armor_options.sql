-- Approved costume options, Santa ruby-only shop and training armor 5.
-- Run while the game is in data-saving maintenance. Re-running is idempotent.
SET NAMES utf8mb4;
-- The requested armor description has 76 characters; legacy VARCHAR(75) truncates its final percent sign.
ALTER TABLE item_template MODIFY COLUMN description VARCHAR(255) NULL DEFAULT NULL;
CREATE TABLE IF NOT EXISTS item_default_option (
    item_id INT NOT NULL,
    option_id INT NOT NULL,
    param INT NOT NULL,
    sort_order SMALLINT NOT NULL,
    PRIMARY KEY (item_id,option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DELIMITER //
DROP PROCEDURE IF EXISTS apply_costume_santa_20261004//
CREATE PROCEDURE apply_costume_santa_20261004()
BEGIN
    DECLARE shopid INT;
    DECLARE costume_tab INT;
    DECLARE armor_tab INT;
    DECLARE rowid INT;
    DECLARE nextrow INT;
    DECLARE done INT DEFAULT 0;
    DECLARE itemid INT;
    DECLARE price INT;
    DECLARE tabid INT;
    DECLARE optiontext VARCHAR(255) DEFAULT 'Khi mặc vào sẽ đánh yếu hơn bình thường 40%, khi cởi ra sẽ tăng sức đánh 50%';
    DECLARE items CURSOR FOR
        SELECT 710,1000 UNION ALL SELECT 711,1500 UNION ALL SELECT 1710,2000 UNION ALL SELECT 1869,2500;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    IF EXISTS (SELECT 1 FROM item_option_template WHERE id=251 AND BINARY NAME<>BINARY optiontext) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Option 251 already belongs to another effect; refusing overwrite';
    END IF;
    IF (SELECT COUNT(*) FROM item_template WHERE id IN (710,711,1710,1869))<>4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Required costume/armor templates missing';
    END IF;
    IF (SELECT COUNT(*) FROM shop WHERE tag_name='SANTA_HONG_NGOC')>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate Santa ruby shop tags';
    END IF;

    START TRANSACTION;
    INSERT INTO item_option_template(id,NAME) VALUES(251,optiontext)
        ON DUPLICATE KEY UPDATE NAME=VALUES(NAME);
    INSERT INTO item_default_option(item_id,option_id,param,sort_order) VALUES
        (710,50,22,0),(710,77,20,1),(710,103,20,2),(710,159,3,3),(710,160,35,4),
        (711,50,23,0),(711,77,21,1),(711,103,21,2),(711,159,4,3),(711,160,50,4),
        (1710,50,24,0),(1710,77,22,1),(1710,103,22,2),(1710,159,5,3),(1710,160,60,4),
        (1869,77,15,0),(1869,103,15,1),(1869,94,5,2),(1869,47,50000,3),(1869,9,1000,4),(1869,251,0,5)
        ON DUPLICATE KEY UPDATE param=VALUES(param),sort_order=VALUES(sort_order);
    UPDATE item_template SET description='Cải trang thành Quy lão Kame' WHERE id=710;
    UPDATE item_template SET description='Cải trang thành Jacky Chun' WHERE id=711;
    UPDATE item_template SET description='Cải trang thành Android 21 Tiến Sĩ',head=1603,body=1606,leg=1607 WHERE id=1710;
    UPDATE item_template SET description=optiontext WHERE id=1869;
    INSERT INTO head_avatar(head_id,avatar_id) SELECT 1603,14241
        WHERE NOT EXISTS (SELECT 1 FROM head_avatar WHERE head_id=1603);

    SET shopid=(SELECT MIN(id) FROM shop WHERE tag_name='SANTA_HONG_NGOC');
    IF shopid IS NULL THEN
        SET shopid=(SELECT COALESCE(MAX(id),0)+1 FROM shop);
        INSERT INTO shop(id,npc_id,tag_name,type_shop) VALUES(shopid,39,'SANTA_HONG_NGOC',3);
    ELSE
        UPDATE shop SET npc_id=39,type_shop=3 WHERE id=shopid;
    END IF;
    SET costume_tab=(SELECT MIN(id) FROM tab_shop WHERE shop_id=shopid AND NAME='Cải trang');
    IF costume_tab IS NULL THEN
        SET costume_tab=(SELECT COALESCE(MAX(id),0)+1 FROM tab_shop);
        INSERT INTO tab_shop(id,shop_id,NAME) VALUES(costume_tab,shopid,'Cải trang');
    END IF;
    SET armor_tab=(SELECT MIN(id) FROM tab_shop WHERE shop_id=shopid AND NAME='Giáp tập luyện');
    IF armor_tab IS NULL THEN
        SET armor_tab=(SELECT COALESCE(MAX(id),0)+1 FROM tab_shop);
        INSERT INTO tab_shop(id,shop_id,NAME) VALUES(armor_tab,shopid,'Giáp tập luyện');
    END IF;
    DELETE o FROM item_shop_option o JOIN item_shop s ON s.id=o.item_shop_id
        WHERE s.tab_id IN (costume_tab,armor_tab) AND s.temp_id IN (710,711,1710,1869);
    OPEN items;
    item_loop: LOOP
        FETCH items INTO itemid,price;
        IF done=1 THEN LEAVE item_loop; END IF;
        SET tabid=IF(itemid=1869,armor_tab,costume_tab);
        SET rowid=(SELECT MIN(id) FROM item_shop WHERE tab_id=tabid AND temp_id=itemid);
        IF rowid IS NULL THEN
            SET rowid=(SELECT COALESCE(MAX(id),0)+1 FROM item_shop);
            INSERT INTO item_shop(id,tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,create_time)
                VALUES(rowid,tabid,itemid,1,1,3,price,7743,NOW());
        ELSE
            UPDATE item_shop SET is_sell=1,type_sell=3,cost=price,icon_spec=7743 WHERE id=rowid;
        END IF;
        DELETE FROM item_shop_option WHERE item_shop_id=rowid;
        SET nextrow=(SELECT COALESCE(MAX(id),0) FROM item_shop_option);
        INSERT INTO item_shop_option(id,item_shop_id,option_id,param)
            SELECT nextrow+ROW_NUMBER() OVER(ORDER BY sort_order,option_id),rowid,option_id,param
            FROM item_default_option WHERE item_id=itemid;
    END LOOP;
    CLOSE items;
    COMMIT;
END//
CALL apply_costume_santa_20261004()//
DROP PROCEDURE apply_costume_santa_20261004//
DELIMITER ;
