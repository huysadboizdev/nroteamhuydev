-- HIGH_TIER_EQUIPMENT_POWER_LOCK_20261006
-- Values are billions in option21; never place >INT_MAX in template power_require.
START TRANSACTION;
DELETE o FROM item_shop_option o JOIN item_shop i ON i.id=o.item_shop_id
WHERE (i.temp_id BETWEEN 650 AND 662 OR i.temp_id BETWEEN 1048 AND 1062)
  AND o.option_id IN (21,30);
INSERT INTO item_shop_option (item_shop_id,option_id,param)
SELECT i.id,21,CASE
 WHEN i.temp_id BETWEEN 1048 AND 1062 THEN 80
 WHEN i.temp_id IN (650,652,654) THEN 40
 WHEN i.temp_id IN (651,653,655) THEN 44
 WHEN i.temp_id IN (657,659,661) THEN 50
 WHEN i.temp_id IN (658,660,662) THEN 43
 WHEN i.temp_id=656 THEN 47 END
FROM item_shop i
WHERE i.temp_id BETWEEN 650 AND 662 OR i.temp_id BETWEEN 1048 AND 1062;
INSERT INTO item_shop_option (item_shop_id,option_id,param)
SELECT i.id,30,0 FROM item_shop i
WHERE i.temp_id BETWEEN 650 AND 662 OR i.temp_id BETWEEN 1048 AND 1062;
COMMIT;
