-- Move Daishinkan away from the Mori house entrance (map 7).
-- Idempotent: preserve every other NPC and only replace the old coordinates.
UPDATE map_template
SET npcs = REPLACE(REPLACE(npcs,
    '[111, 454, 432]', '[111, 850, 432]'),
    '[111,454,432]', '[111,850,432]')
WHERE id = 7;
