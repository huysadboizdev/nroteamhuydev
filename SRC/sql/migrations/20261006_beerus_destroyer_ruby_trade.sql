-- BEERUS_DESTROYER_RUBY_TRADE_20261006
-- Scope exclusively to Beerus; preserve rows, tabs and base item options.
START TRANSACTION;
UPDATE shop SET type_shop=3 WHERE tag_name='BILL' AND npc_id=55;
UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id
SET i.type_sell=3,i.cost=100,i.icon_spec=7743
WHERE s.tag_name='BILL' AND s.npc_id=55;
COMMIT;
