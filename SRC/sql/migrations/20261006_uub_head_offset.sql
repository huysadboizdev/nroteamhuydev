-- Align Uub head with body using existing client CharInfo coordinates.
START TRANSACTION;
UPDATE part SET DATA='[[8586,1,3],[8587,-2,3],[2955,0,0]]' WHERE id=946;
COMMIT;
