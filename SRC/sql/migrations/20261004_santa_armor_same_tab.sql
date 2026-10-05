-- Place training armor 5 beside the three costumes without changing its ID, options or price.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE item_shop i
JOIN tab_shop previous_tab ON previous_tab.id=i.tab_id
JOIN shop s ON s.id=previous_tab.shop_id AND s.tag_name='SANTA_HONG_NGOC'
JOIN tab_shop costume_tab ON costume_tab.shop_id=s.id AND costume_tab.NAME='Cải trang'
SET i.tab_id=costume_tab.id
WHERE i.temp_id=1869;
DELETE t FROM tab_shop t
JOIN shop s ON s.id=t.shop_id AND s.tag_name='SANTA_HONG_NGOC'
LEFT JOIN item_shop i ON i.tab_id=t.id
WHERE t.NAME='Giáp tập luyện' AND i.id IS NULL;
COMMIT;
