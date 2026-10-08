-- Santa green-gem ticket; VIP rewards are supplied by VipRewardCatalog.
-- Keep existing item/radar IDs: no duplicate card or ticket templates.
START TRANSACTION;
UPDATE item_template SET NAME='Vé nhiệm vụ KOL VIP',
 description='Vé dùng để nhận nhiệm vụ KOL VIP. Cần có vé trong hành trang.',
 gem=1000 WHERE id=1825;
-- Remove any duplicate ticket rows and their options before updating the survivor.
DELETE opt FROM item_shop_option opt
 JOIN item_shop duplicate ON duplicate.id=opt.item_shop_id
 WHERE duplicate.tab_id=17 AND duplicate.temp_id=1825
 AND duplicate.id<>(SELECT keep_id FROM
  (SELECT MIN(id) AS keep_id FROM item_shop WHERE tab_id=17 AND temp_id=1825) keep_row);
DELETE FROM item_shop WHERE tab_id=17 AND temp_id=1825
 AND id<>(SELECT keep_id FROM
  (SELECT MIN(id) AS keep_id FROM item_shop WHERE tab_id=17 AND temp_id=1825) keep_row);
SET @santa_kol_vip_shop_id=(SELECT COALESCE(MAX(id),0)+1 FROM item_shop);
INSERT INTO item_shop(id,tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,create_time)
 SELECT @santa_kol_vip_shop_id,17,1825,1,1,1,1000,0,NOW()
 WHERE NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=17 AND temp_id=1825);
UPDATE item_shop SET is_new=1,is_sell=1,type_sell=1,cost=1000,icon_spec=0
 WHERE tab_id=17 AND temp_id=1825;
COMMIT;
