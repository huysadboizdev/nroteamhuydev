# Buu, equipment and Kurama implementation plan

**Goal:** implement requested percentages and repair companion use while preserving previous Osin/Kilis fixes.

**Architecture:** shared equipped-level policy in player model; NPoint consumes it once per stat calculation; UseItem uses CompanionCatalog; existing effect protocol supplies visuals.

**Tech Stack:** Java 17 bytecode/JDK21, Unity C#, Python build/deployment scripts.

- [x] Write regression tests against real NPoint and inventory/use paths; run against installed JAR and record expected failures.
- [x] Implement Buu 10% and shared full-set upgrade policy; run tests for mixed levels, missing pieces, refresh, unfusion and existing VIPs.
- [x] Repair catalog companion use; test equip, replacement, power rejection and removal.
- [x] Verify aura assets/protocol; fix stale client frames and equipment refresh if necessary; compile client.
- [x] Build candidate, run new and existing regressions; inspect changed classes and unaffected JAR entries.
- [x] Deploy with saved maintenance and hashed rollback backup; restart and verify installed JAR/server.

- [x] Complete and verify new Android APK distribution.
