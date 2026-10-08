-- Preload the existing Noi Banh NPC beside the existing Vua Hung NPC.
-- The verified codec is direct rows [templateId,x,y]. Remove whitespace only for
-- the membership check; JSON_ARRAY_APPEND preserves the other NPC rows.
UPDATE map_template SET npcs=JSON_ARRAY_APPEND(npcs,'$',JSON_ARRAY(66,777,480))
 WHERE id=183 AND JSON_VALID(npcs) AND REPLACE(npcs,' ','') NOT LIKE '%[66,%';
UPDATE map_template SET npcs=JSON_ARRAY_APPEND(npcs,'$',JSON_ARRAY(66,215,624))
 WHERE id=184 AND JSON_VALID(npcs) AND REPLACE(npcs,' ','') NOT LIKE '%[66,%';
UPDATE map_template SET npcs=JSON_ARRAY_APPEND(npcs,'$',JSON_ARRAY(66,801,672))
 WHERE id=185 AND JSON_VALID(npcs) AND REPLACE(npcs,' ','') NOT LIKE '%[66,%';
