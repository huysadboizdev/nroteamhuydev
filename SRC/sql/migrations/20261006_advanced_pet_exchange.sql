-- Original res moi item 1890/icon 15767, reserved blank slot in this server.
START TRANSACTION;
UPDATE item_template SET TYPE=75,gender=3,NAME='Đổi đệ tử 2',
 description='Đổi ngẫu nhiên sang Uub, Berus nhí hoặc Jiren khác loại hiện tại. Đệ mới bắt đầu ở 40 tỉ sức mạnh, chỉ số và kỹ năng được tạo lại. Cần tháo trang bị đệ trước khi dùng.',
 level=0,icon_id=15767,part=-1,is_up_to_up=1,power_require=0,gold=0,gem=100,head=-1,body=-1,leg=-1
 WHERE id=1890 AND (NAME='' OR NAME='Đổi đệ tử 2');
SET @advanced_pet_shop_id=(SELECT COALESCE(MAX(id),0)+1 FROM item_shop);
INSERT INTO item_shop(id,tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,create_time)
 SELECT @advanced_pet_shop_id,17,1890,1,1,1,100,0,NOW()
 WHERE NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=17 AND temp_id=1890);
UPDATE item_shop SET is_new=1,is_sell=1,type_sell=1,cost=100,icon_spec=0 WHERE tab_id=17 AND temp_id=1890;
UPDATE head_avatar SET avatar_id=8617 WHERE head_id=946;
COMMIT;
