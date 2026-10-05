-- Correct the dragon pet icon; keep Ghost Rider (1900) at icon 16187.
START TRANSACTION;
UPDATE item_template SET icon_id = 15125
WHERE id = 1771 AND icon_id = 16187
  AND head = 1680 AND body = 1681 AND leg = 1682;
COMMIT;
