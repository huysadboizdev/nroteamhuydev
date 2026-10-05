-- EQUIPMENT_SEAL_MARKET_20261005
-- SQL policy only: enforce in Java before exposing sealed items on consignment.
-- 30 = no direct trade; 87 = gem consignment display (settlement must be ruby).
-- Existing green-gem listings must never be reinterpreted as ruby listings.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_market_rule (
    seal_option_id INT NOT NULL PRIMARY KEY,
    direct_trade_allowed TINYINT NOT NULL,
    trade_lock_option_id INT NOT NULL,
    consign_option_id INT NOT NULL,
    consign_currency VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
START TRANSACTION;
INSERT INTO equipment_seal_market_rule
    (seal_option_id,direct_trade_allowed,trade_lock_option_id,consign_option_id,consign_currency)
VALUES (34,0,30,87,'ruby'),(35,0,30,87,'ruby'),(36,0,30,87,'ruby')
ON DUPLICATE KEY UPDATE
    direct_trade_allowed=VALUES(direct_trade_allowed),
    trade_lock_option_id=VALUES(trade_lock_option_id),
    consign_option_id=VALUES(consign_option_id),
    consign_currency=VALUES(consign_currency);
COMMIT;
