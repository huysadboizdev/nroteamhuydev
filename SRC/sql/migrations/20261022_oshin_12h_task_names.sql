SET NAMES utf8mb4;

UPDATE task_sub_template
SET `name` = REPLACE(`name`, 'Pui Pui', 'Bui Bui'),
    `notify` = REPLACE(`notify`, 'Pui Pui', 'Bui Bui')
WHERE task_main_id = 28
  AND ducvupro IN (119, 120);
