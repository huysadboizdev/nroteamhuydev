-- PET_SKILL_REROLL_RUBY_SHOP_20261016
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO item_template
(id,TYPE,gender,NAME,description,level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg)
VALUES
(2042,29,3,CONVERT(0x506869e1babf7520c491e1bb9569206be1bbb9206ec4836e6720322d3320c491e1bb872074e1bbad USING utf8mb4),CONVERT(0x59c3aa752063e1baa77520c491e1bb872074e1bbad20c491c3a32068e1bb8d63206be1bbb9206ec4836e6720322076c3a0206be1bbb9206ec4836e6720332e USING utf8mb4),1,5223,-1,1,0,0,0,-1,-1,-1),
(2043,29,3,CONVERT(0x506869e1babf7520c491e1bb9569206be1bbb9206ec4836e67203420c491e1bb872074e1bbad USING utf8mb4),CONVERT(0x59c3aa752063e1baa77520c491e1bb872074e1bbad20c491c3a32068e1bb8d63206be1bbb9206ec4836e6720342e20c490e1bb9569207468c3a06e68204269e1babf6e204b68e1bb892e USING utf8mb4),1,718,-1,1,0,0,0,-1,-1,-1)
ON DUPLICATE KEY UPDATE
 TYPE=VALUES(TYPE),gender=VALUES(gender),NAME=VALUES(NAME),description=VALUES(description),
 level=VALUES(level),icon_id=VALUES(icon_id),part=VALUES(part),is_up_to_up=VALUES(is_up_to_up),
 power_require=VALUES(power_require),gold=VALUES(gold),gem=VALUES(gem),
 head=VALUES(head),body=VALUES(body),leg=VALUES(leg);

SET @pet_skill_shop=(SELECT id FROM shop WHERE tag_name='SANTA_HONG_NGOC' AND npc_id=39 LIMIT 1);
SET @pet_skill_tab=(
 SELECT MIN(t.id) FROM tab_shop t
 JOIN shop s ON s.id=t.shop_id
 WHERE s.id=@pet_skill_shop AND t.name NOT LIKE CONVERT(0x48e1bb99703c3e25 USING utf8mb4)
);

INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec)
SELECT @pet_skill_tab,2042,1,1,3,100,7743
WHERE @pet_skill_tab IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=@pet_skill_tab AND temp_id=2042);
INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec)
SELECT @pet_skill_tab,2043,1,1,3,2000,7743
WHERE @pet_skill_tab IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM item_shop WHERE tab_id=@pet_skill_tab AND temp_id=2043);

UPDATE item_shop SET is_new=1,is_sell=1,type_sell=3,cost=100,icon_spec=7743
WHERE tab_id=@pet_skill_tab AND temp_id=2042;
UPDATE item_shop SET is_new=1,is_sell=1,type_sell=3,cost=2000,icon_spec=7743
WHERE tab_id=@pet_skill_tab AND temp_id=2043;

COMMIT;
