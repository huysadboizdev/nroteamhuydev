-- Restore Uub parts to existing verified icons; keep original offsets and other costumes.
START TRANSACTION;
UPDATE part SET DATA='[[8586,1,-4],[8587,-2,-4],[2955,0,0]]' WHERE id=946;
UPDATE part SET DATA='[[8589,2,-1],[8588,-1,-2],[8590,0,-3],[8591,0,-2],[8592,0,-2],[8593,0,-1],[8594,0,-1],[8595,2,0],[8596,2,0],[8597,2,0],[8598,5,0],[8599,1,-1],[8600,2,-1],[8601,2,-2],[8602,3,1],[8603,3,-1],[2955,0,0]]' WHERE id=947;
UPDATE part SET DATA='[[8605,0,0],[8604,0,1],[8606,0,0],[8607,0,0],[8608,0,0],[8609,0,0],[8610,0,0],[8611,1,1],[8612,1,3],[8613,1,0],[8614,1,0],[8615,1,2],[8616,1,1],[2955,0,0]]' WHERE id=948;
UPDATE head_avatar SET avatar_id=8586 WHERE head_id=946;
COMMIT;
