-- Chi truy van doc. Chay trong database team2026.
SELECT * FROM item_template
WHERE id IN (678,710,711,976,1385,1602,1708,1709,1710);

SELECT s.temp_id,s.id AS item_shop_id,s.tab_id,s.is_new,s.is_sell,
       s.type_sell,s.cost,s.icon_spec,o.id AS option_row_id,
       o.option_id,o.param,t.NAME AS option_name
FROM item_shop AS s
LEFT JOIN item_shop_option AS o ON o.item_shop_id=s.id
LEFT JOIN item_option_template AS t ON t.id=o.option_id
WHERE s.temp_id IN (678,710,711,976,1385,1602,1708,1709,1710);

SELECT * FROM shop_ky_gui
WHERE item_id IN (678,710,711,976,1385,1602,1708,1709,1710);

SELECT * FROM data_badges
WHERE idItem IN (678,710,711,976,1385,1602,1708,1709,1710);
