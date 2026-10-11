# Goku Day Fix Implementation Plan

**Goal:** Repair SSJ God appearance and the audited opening defects for gift1591/1594.

**Architecture:** Targeted shared-handler changes plus an idempotent template1593 SQL migration; use existing genuine Goku sprites. Patch only UseItem classes in a candidate copied from the current JAR.

**Tech Stack:** Java17 source/Java21 toolchain, standalone Java tests, MariaDB, Python deployment scripts.

- [x] Add `SRC/test/nro/models/services/GokuDayGiftTest.java`, exercise the actual shared private handler with deterministic Random seeds and mocked service boundaries. Run against current JAR and capture the permanent reward failure.
- [x] Change only `OpenHopQuaGokuDayVangNgoc`: Random.nextInt(1000), add option93=3 only for roll>=5, correct the free-slot message. Run tests for both boxes on roll0,4,5,999, rejected insertion and insufficient slots.
- [x] Add migration `SRC/sql/migrations/20261010_goku_day_god_appearance.sql`, updating only item1593 body1450/leg1451 after checking unique valid part/type data. Run twice on isolated template tables; preserve unrelated rows.
- [x] Compile all Java sources. Create candidate in `SRC/jar-backup` replacing only UseItem classes; rerun Goku tests and relevant item/expiry regressions on candidate.
- [x] Save SQL, seed, runtime part and JAR hashes; maintenance-save and stop the server; append migration to four seeds, apply live SQL, archive old JAR before replacing it, and verify both hashes.
- [x] Start server hidden, read runtime item1593 and village NPC positions, rerun the resource audit, verify deployment hashes, write release evidence.
