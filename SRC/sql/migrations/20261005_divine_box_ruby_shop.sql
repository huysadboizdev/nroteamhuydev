-- DIVINE_BOX_1775_RUBY_SHOP_20261005
-- Box 1775 chooses one complete deterministic, nontradable divine set.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE item_template SET name='Hộp đồ Thần Linh',
 description='Chọn 1 bộ Thần Linh Namếc, Trái Đất hoặc Xayda gồm 5 món chỉ số gốc 0%. Không giao dịch, không ký gửi.'
WHERE id=1775;
SET @divine_ruby_shop=(SELECT id FROM shop WHERE tag_name='SANTA_HONG_NGOC' AND npc_id=39 LIMIT 1);
INSERT INTO tab_shop(shop_id,name)
SELECT @divine_ruby_shop,'Hộp<>Trang bị' WHERE @divine_ruby_shop IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM tab_shop WHERE shop_id=@divine_ruby_shop AND name='Hộp<>Trang bị');
SET @divine_box_tab=(SELECT id FROM tab_shop WHERE shop_id=@divine_ruby_shop AND name='Hộp<>Trang bị' LIMIT 1);
INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec)
SELECT @divine_box_tab,1775,1,1,3,50000,7743 WHERE @divine_box_tab IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=@divine_box_tab AND temp_id=1775);
UPDATE item_shop SET is_new=1,is_sell=1,type_sell=3,cost=50000,icon_spec=7743
WHERE tab_id=@divine_box_tab AND temp_id=1775;
DELETE FROM item_shop_option WHERE item_shop_id IN
 (SELECT id FROM item_shop WHERE tab_id=@divine_box_tab AND temp_id=1775) AND option_id IN(30,86,87);
INSERT INTO item_shop_option(item_shop_id,option_id,param)
SELECT id,30,1 FROM item_shop WHERE tab_id=@divine_box_tab AND temp_id=1775;
COMMIT;
