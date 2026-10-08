-- Offline Namek rewards update only items_bag, atomically with this receipt.
CREATE TABLE IF NOT EXISTS namek_wish_receipt (
    session_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    player_id BIGINT NOT NULL,
    wish TINYINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(session_id,player_id)
) ENGINE=InnoDB;
