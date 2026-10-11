# Ngũ Hành Sơn Implementation Plan

> Execute inline with executing-plans; the user authorized implementation and chose peaches and x5 on all three maps.

**Goal:** Đường Tăng in all starter villages leads to three populated Ngũ Hành Sơn maps; the last has Đường Tăng and imprisoned Ngộ Không with permanent costume exchanges.

**Architecture:** Reuse maps 122–124, NPC templates 48/49, existing mobs and validated costume art. Dedicated service owns exchange prices and region travel; one NPC implementation handles both guides. Preserve other NPCs and mob populations. Connect maps in order 122 → 123 → 124 and return through the guides. Multiply TNSM with saturating arithmetic.

**Tech Stack:** Java 17, MariaDB, server binary parts and PNG icons, Unity resource fallback.

- [x] Audit item/NPC parts, binary part records, four zoom levels and client path resolution. Only offer complete costumes; report absent Sa Ngộ Tĩnh without inventing art.
- [x] Add `SRC/test/nro/models/services/NguHanhSonTest.java`: actual NPoint returns 5x for 122/123/124, unchanged outside, saturation; exchanges reject insufficient/mismatched currency/full bag/failed grant and consume exact split stacks once. Run against old JAR for RED.
- [x] Implement `NguHanhSonService.java`, `TayDuGuide.java`, register 48/49 in NpcFactory, change NPoint multiplier. Permanent ordinary Wukong 544–546 costs 200 peaches with 20% SD/HP/KI; follower 547/548 costs 150 ripe peaches with 20%; special 1698 costs 300 ripe peaches with 25%. Keep follower-only templates labelled explicitly.
- [x] Add migration `20261010_ngu_hanh_son.sql`: append NPC49 at equal 82px offset beyond village guide groups; move 48/49 to final124, fix linear bidirectional route, retain all monsters. Test isolated import, idempotence and preservation before live application.
- [x] Compile all source; package only modified/new classes. Run feature tests and existing gift/inventory regressions on candidate. Inspect diff and resource audit.
- [x] Graceful maintenance/save, SQL backup, old JAR timestamped backup and SHA256 metadata; apply SQL plus four seed files; install verified candidate, restart hidden and verify loaded maps/NPCs/reward templates.
- [x] Export fresh full SQL, validate restore/table counts, write release report and concrete prices/asset limitations.

Verified installed JAR and JVM after final restart; legacy string NPC rows normalized. Final full export: SRC/sql/team2026-full-20261010-214007.sql (75 tables, restore verified). Unity source compiled; existing EXE packages were not rebuilt.
