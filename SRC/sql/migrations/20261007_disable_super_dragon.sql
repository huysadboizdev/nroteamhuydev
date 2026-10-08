-- DISABLE_SUPER_DRAGON_20261007
-- Keep template ID1015: deleting a row would shift positional catalog lookups.
UPDATE item_shop SET is_sell = 0 WHERE temp_id = 1015;
UPDATE item_template SET description = 'Vật phẩm tạm ngưng sử dụng.' WHERE id = 1015;
