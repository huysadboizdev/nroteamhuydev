-- EQUIPMENT_SEAL_ORDINARY_LEVELS_20261005
-- Ordinary seals are limited to equipment levels 10, 11, 12 (not item IDs).
-- Configuration only; generation and set-stat runtime remain separate work.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO equipment_seal_tier_item (item_id,equipment_tier) VALUES
    (231,'ordinary'),(232,'ordinary'),(233,'ordinary'),
    (235,'ordinary'),(236,'ordinary'),(237,'ordinary'),
    (239,'ordinary'),(240,'ordinary'),(241,'ordinary'),
    (243,'ordinary'),(244,'ordinary'),(245,'ordinary'),
    (247,'ordinary'),(248,'ordinary'),(249,'ordinary'),
    (251,'ordinary'),(252,'ordinary'),(253,'ordinary'),
    (255,'ordinary'),(256,'ordinary'),(257,'ordinary'),
    (259,'ordinary'),(260,'ordinary'),(261,'ordinary'),
    (263,'ordinary'),(264,'ordinary'),(265,'ordinary'),
    (267,'ordinary'),(268,'ordinary'),(269,'ordinary'),
    (271,'ordinary'),(272,'ordinary'),(273,'ordinary'),
    (275,'ordinary'),(276,'ordinary'),(277,'ordinary'),
    (279,'ordinary'),(280,'ordinary'),(281,'ordinary')
ON DUPLICATE KEY UPDATE equipment_tier=VALUES(equipment_tier);
COMMIT;
