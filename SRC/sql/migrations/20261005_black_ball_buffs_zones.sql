-- Re-runnable; only event map configuration and item descriptions are changed.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE map_template SET zones = 2, max_player = 50 WHERE id IN (85,86,87);
UPDATE map_template SET zones = 3, max_player = 50 WHERE id IN (88,89,90,91);
UPDATE item_template SET description = '+21% sức đánh cho toàn bang' WHERE id = 372;
UPDATE item_template SET description = '+35% HP tối đa cho toàn bang' WHERE id = 373;
UPDATE item_template SET description = '+35% KI tối đa cho toàn bang' WHERE id = 374;
UPDATE item_template SET description = '+35% phản sát thương cho toàn bang' WHERE id = 375;
UPDATE item_template SET description = '+35% hút HP theo sát thương cho toàn bang' WHERE id = 376;
UPDATE item_template SET description = '+20% giáp giảm sát thương cho toàn bang' WHERE id = 377;
UPDATE item_template SET description = '+14% né đòn cho toàn bang' WHERE id = 378;
COMMIT;
