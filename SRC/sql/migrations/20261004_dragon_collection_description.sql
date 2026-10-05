START TRANSACTION;
UPDATE item_template
SET description = 'Sở hữu 7 Pet Rồng Nhí Vĩnh Viễn sẽ tăng 10% HP, KI, Sức Đánh'
WHERE id BETWEEN 1765 AND 1771;
COMMIT;
