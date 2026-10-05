-- EQUIPMENT_SEAL_RULES_20261005
-- Configuration only. Runtime/drop support requires the planned Java update.
-- ordinary: random 1..5 per item, 15 percent full set.
-- divine: random 1..7 per item, 20 percent full divine set.
-- Base-stat premium over the unsealed baseline: ordinary 5..10, divine 5..15.
-- Existing item templates/options/player data are preserved.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_rule (
    equipment_tier VARCHAR(16) NOT NULL,
    seal_option_id INT NOT NULL,
    bonus_option_id INT NOT NULL,
    min_percent INT NOT NULL,
    max_percent INT NOT NULL,
    set_percent INT NOT NULL,
    PRIMARY KEY (equipment_tier, seal_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_tier_item (
    item_id INT NOT NULL PRIMARY KEY,
    equipment_tier VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_base_rule (
    equipment_tier VARCHAR(16) NOT NULL PRIMARY KEY,
    min_percent INT NOT NULL,
    max_percent INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
START TRANSACTION;
INSERT INTO equipment_seal_rule
    (equipment_tier,seal_option_id,bonus_option_id,min_percent,max_percent,set_percent)
VALUES
    ('ordinary',34,50,1,5,15),
    ('ordinary',35,103,1,5,15),
    ('ordinary',36,77,1,5,15),
    ('divine',34,50,1,7,20),
    ('divine',35,103,1,7,20),
    ('divine',36,77,1,7,20)
ON DUPLICATE KEY UPDATE
    bonus_option_id=VALUES(bonus_option_id),
    min_percent=VALUES(min_percent),
    max_percent=VALUES(max_percent),
    set_percent=VALUES(set_percent);
INSERT INTO equipment_seal_tier_item (item_id,equipment_tier) VALUES
    (555,'divine'),(556,'divine'),(557,'divine'),(558,'divine'),
    (559,'divine'),(560,'divine'),(561,'divine'),(562,'divine'),
    (563,'divine'),(564,'divine'),(565,'divine'),(566,'divine'),(567,'divine')
ON DUPLICATE KEY UPDATE equipment_tier=VALUES(equipment_tier);
INSERT INTO equipment_seal_base_rule (equipment_tier,min_percent,max_percent)
VALUES ('ordinary',5,10),('divine',5,15)
ON DUPLICATE KEY UPDATE
    min_percent=VALUES(min_percent),max_percent=VALUES(max_percent);
COMMIT;
