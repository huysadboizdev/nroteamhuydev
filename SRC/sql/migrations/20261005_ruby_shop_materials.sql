-- RUBY_SHOP_MATERIALS_SINGLE_TAB_20261005
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE item_template SET description='Mở ra được set Thần Linh' WHERE id=1775;
SET @ruby_shop=(SELECT id FROM shop WHERE tag_name='SANTA_HONG_NGOC' AND npc_id=39 LIMIT 1);
SET @ruby_tab=(SELECT MIN(id) FROM tab_shop WHERE shop_id=@ruby_shop AND name<>'Hộp<>Trang bị');
-- Reuse the existing shop tab, never create a separate materials/box branch.
UPDATE tab_shop SET name='Cửa<>hàng' WHERE id=@ruby_tab;
UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id
SET i.tab_id=@ruby_tab WHERE t.shop_id=@ruby_shop AND i.temp_id IN(1775,933,934,1820,935) AND @ruby_tab IS NOT NULL;
DELETE t FROM tab_shop t LEFT JOIN item_shop i ON i.tab_id=t.id
WHERE t.shop_id=@ruby_shop AND t.name='Hộp<>Trang bị' AND i.id IS NULL AND @ruby_tab IS NOT NULL;
INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec)
SELECT @ruby_tab,m.id,1,1,3,m.price,7743 FROM
 (SELECT 933 id,100 price UNION ALL SELECT 934,100 UNION ALL SELECT 1820,100 UNION ALL SELECT 935,10) m
WHERE @ruby_tab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=@ruby_tab AND temp_id=m.id);
UPDATE item_shop SET is_new=1,is_sell=1,type_sell=3,cost=IF(temp_id=935,10,100),icon_spec=7743
WHERE tab_id=@ruby_tab AND temp_id IN(933,934,1820,935);
-- Upgrade services count fragments in option31; item quantity remains one.
DELETE o FROM item_shop_option o JOIN item_shop i ON i.id=o.item_shop_id
WHERE i.tab_id=@ruby_tab AND i.temp_id IN(933,934,1820,935);
INSERT INTO item_shop_option(item_shop_id,option_id,param)
SELECT id,31,1000 FROM item_shop WHERE tab_id=@ruby_tab AND temp_id IN(933,934,1820);
COMMIT;
