-- RUBY_OCTOPUS_20261007
SET NAMES utf8mb4;
START TRANSACTION;
SET @ruby_shop=(SELECT id FROM shop WHERE tag_name='SANTA_HONG_NGOC' AND npc_id=39 LIMIT 1);
SET @ruby_tab=(SELECT MIN(id) FROM tab_shop WHERE shop_id=@ruby_shop);
INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec)
SELECT @ruby_tab,881,1,1,3,5,7743 FROM item_template
WHERE id=881 AND @ruby_tab IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=@ruby_tab AND temp_id=881);
UPDATE item_shop SET is_new=1,is_sell=1,type_sell=3,cost=5,icon_spec=7743
WHERE tab_id=@ruby_tab AND temp_id=881;
COMMIT;
