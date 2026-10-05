-- Capsule722 và vé2000; dọn capsule cũ bằng RubyDatabaseOps trong bảo trì.
CREATE TABLE IF NOT EXISTS ruby_gift_receipt (request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,sender_id BIGINT NOT NULL,receiver_id BIGINT NOT NULL,sender_account INT NOT NULL,receiver_account INT NOT NULL,amount INT NOT NULL,fee INT NOT NULL,net INT NOT NULL,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,KEY sender_created(sender_id,created_at),KEY receiver_created(receiver_id,created_at)) ENGINE=InnoDB;

UPDATE item_template SET description='Mở nhận 1–100 hồng ngọc + 1 vé tặng. 80% nhận 1–5 hồng ngọc.' WHERE id=722;

INSERT INTO item_template
SELECT 2000,type,gender,'Vé tặng hồng ngọc','Tặng hồng ngọc cho người chơi online. Mất 1 vé/lần, phí 10%.',level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg
FROM item_template WHERE id=718 AND NOT EXISTS (SELECT 1 FROM item_template WHERE id=2000);

DELETE o FROM item_shop_option o JOIN item_shop s ON s.id=o.item_shop_id
WHERE s.temp_id=722 AND o.option_id=97;
