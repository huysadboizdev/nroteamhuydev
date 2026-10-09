# Daishinkan Ranking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement task-by-task with spec and quality review.

**Goal:** Disable home Bo Mong and add village Daishinkan with compact power/task rankings.

**Architecture:** Reuse server NPC templates and ranking packet -96; isolate ranking queries/formatting in VillageRankingService. Map initialization guards home Bo Mong; idempotent SQL adds village NPC/template based on verified original art.

**Tech Stack:** Java 21, MariaDB, existing JSON-simple/JDBC, Unity existing NPC renderer.

## Task 1: Rankings
Files: create `SRC/src/nro/models/services/VillageRankingService.java`, `SRC/test/nro/models/services/VillageRankingTest.java`.
- [x] Write tests first: `formatPower(59400007888L) == "59,4 tỷ"`, boundary/long values, compact task, stable descending progress, malformed JSON and packet row count.
- [x] Run tests and observe failure against missing service.
- [x] Implement dedicated JDBC projection and immutable top rows, 60-second cache, bounded top 100, packet -96 version head field, title per category, error message if query unavailable.
- [x] Run tests against sources and staged JAR.

## Task 2: NPC and content
Files: `ConstNpc.java`, `NpcFactory.java`, `Map.java`, new `npc_list/Daishinkan.java`, new SQL migration.
- [x] Locate original Daishinkan head/body/leg/avatar and check part/frame/icon availability at all zoom levels.
- [x] Add tests: home map NPC 17 skipped; other maps retain it; Daishinkan factory/menu has both rank categories; migration is repeatable and avoids overlapping positions.
- [x] Observe failing tests, implement minimal NPC/menu/placement and persist migration into seed SQL.
- [x] Verify migration twice against isolated database and source/packet tests.

## Task 3: Review, build, release
- [x] Review spec compliance and code quality, correct findings.
- [x] Build Java sources with local JDK and library classpath; test candidate with new and existing regression groups.
- [x] Stage candidate based on active 20.jar while preserving unrelated features; record changed entries and SHA-256.
- [x] Gracefully maintain server, dump DB, back up old JAR, apply checked migration, install candidate, restart hidden process.
- [x] Check port 14445, startup logs, live template/map rows and hashes; record deployment and manual visual-test limitations.

Workspace has existing changes; preserve them and use file backups. No git commit requested; .git is read-only in this environment.

Deployment evidence: `docs/daishinkan-ranking-deployment-20261008.md`. Visual client inspection remains manual.
