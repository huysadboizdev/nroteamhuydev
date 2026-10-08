-- Uub portrait: 8586 is the small in-map head, 8617 is the portrait.
START TRANSACTION;
UPDATE head_avatar SET avatar_id=8617 WHERE head_id=946;
COMMIT;
