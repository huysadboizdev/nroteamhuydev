-- Re-runnable; preserve capacity, templates and all player data.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE map_template SET zones = 20 WHERE id = 126;
COMMIT;
