-- Fixed currency/amount policy is also enforced by AchievementService.
-- Existing player progress and received flags are deliberately preserved.
UPDATE achievement_template SET money = 1000;
UPDATE achievement_template SET info2 = 'Đạt 15 triệu sức mạnh' WHERE id = 17;
