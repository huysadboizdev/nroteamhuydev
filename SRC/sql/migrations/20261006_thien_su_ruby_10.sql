-- THIEN_SU_RUBY_10_20261006
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE item_shop i
JOIN tab_shop t ON t.id=i.tab_id
JOIN shop s ON s.id=t.shop_id
SET i.type_sell=3, i.cost=10, i.icon_spec=7743
WHERE s.tag_name='THIEN_SU';
COMMIT;
