CREATE TABLE IF NOT EXISTS dragon_pet_config (
  item_id INT NOT NULL PRIMARY KEY,
  stars TINYINT UNSIGNED NOT NULL,
  permanent_percent TINYINT UNSIGNED NOT NULL,
  min_days TINYINT UNSIGNED NOT NULL,
  max_days TINYINT UNSIGNED NOT NULL,
  options_json TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
START TRANSACTION;
INSERT INTO dragon_pet_config (item_id,stars,permanent_percent,min_days,max_days,options_json) VALUES
(1765,7,10,1,30,'[[77,16],[50,16],[103,16],[236,20],[30,0]]'),
(1766,6,10,1,30,'[[50,18],[5,7],[14,5],[30,0]]'),
(1767,5,10,1,30,'[[50,18],[94,5],[108,7],[30,0]]'),
(1768,4,10,1,30,'[[77,14],[5,7],[14,5],[30,0]]'),
(1769,3,10,1,30,'[[77,18],[94,5],[108,7],[30,0]]'),
(1770,2,10,1,30,'[[77,22],[50,22],[94,8],[5,11],[14,8],[30,0]]'),
(1771,1,10,1,30,'[[77,22],[50,22],[94,8],[5,11],[14,8],[106,0],[30,0]]')
ON DUPLICATE KEY UPDATE stars=VALUES(stars),permanent_percent=VALUES(permanent_percent),
min_days=VALUES(min_days),max_days=VALUES(max_days),options_json=VALUES(options_json);
UPDATE item_template SET name='Rồng nhí 7 sao' WHERE id=1765;
UPDATE item_template SET name='Rồng nhí 6 sao' WHERE id=1766;
UPDATE item_template SET name='Rồng nhí 5 sao' WHERE id=1767;
UPDATE item_template SET name='Rồng nhí 4 sao' WHERE id=1768;
UPDATE item_template SET name='Rồng nhí 3 sao' WHERE id=1769;
UPDATE item_template SET name='Rồng nhí 2 sao' WHERE id=1770;
UPDATE item_template SET name='Rồng nhí 1 sao' WHERE id=1771;
COMMIT;
