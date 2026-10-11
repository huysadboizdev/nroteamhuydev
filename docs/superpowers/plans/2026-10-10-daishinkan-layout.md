# Daishinkan Layout Implementation Plan

**Goal:** Center Daishinkan between the user-confirmed anchor NPCs in all three villages.

**Architecture:** SQL changes only the existing NPC111 coordinate tuple; a temporary JVM tool applies the same three positions without a restart. Original map data and seed files are backed up for rollback.

**Tech Stack:** MariaDB JSON operations, Python deployment orchestration, Java21 attach API.

- [x] Create `SRC/sql/migrations/20261010_center_daishinkan_villages.sql`, validating unique NPC111 and anchors before changing coordinates.
- [x] Run that migration twice on disposable tables copied from live map data; check exact expected coordinates and preservation of every other tuple. Reject a missing anchor without partially modifying other maps.
- [x] Extend `.analysis/daishinkan-layout/NpcLayoutAgent.java` to preflight the complete three-row coordinate plan before applying it; compile with Java21.
- [x] Save timestamped original map tuples, seed copies and JAR hash under `.analysis/daishinkan-layout/`. Apply the tested SQL and append it to all four existing seed/dump files.
- [x] Verify the port14445 listener and independently reread runtime/database positions (the previous server stopped; the restarted server loaded the saved positions at startup). Confirm all other NPC tuples and JAR hash unchanged.
- [x] Record verification, rollback data and the client reentry requirement in `docs/daishinkan-layout-20261010.md`.
