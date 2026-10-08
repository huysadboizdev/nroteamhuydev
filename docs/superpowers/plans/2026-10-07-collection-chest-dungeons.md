# Collection chest and dungeon verification plan

**Goal:** Make NPC82 a separate collection chest with 20 slots, only a full set of 20 permanent items grants total 3 percent HP/KI/damage, fix the chest drawing, and audit dungeon behavior.

**Architecture:** Keep the normal home box separate. Persist items_collection in the same player UPDATE as bag/body/box. Open existing box wire with marker2; use new explicit transfer types11/12/13. Reject timed items (option93 or absolute expire), pending transactions, invalid indices, remote/stale access and full destinations. One unit per collection slot; no consumption when rejected. Recalculate bonus from current collection contents, never increment persisted percentages. Client draws chest NPC82 once from full icon15288 at bottom center.

**Tech stack:** Java17/JSON-simple/MySQL; Unity C# Game1/Game2.

- [x] Reproduce missing chest behavior on installed JAR.
- [x] Implement/test permanent rules, exact20 threshold, 3 percent stats, item JSON roundtrip.
- [x] Add dedicated transfers/open packets, physical access and concurrency guards; test actual inventory mutations and packet bytes.
- [x] Wire NPC factory, UseItem routing, player load/save and 20-slot inventory.
- [x] Add idempotent schema migration; test twice on isolated schema with unchanged original bag/box.
- [x] Fix client NPC drawing and collection mode in both tabs; compile client and test actual mode/routing/drawing branches.
- [x] Apply stated permanent-only rule to all item categories: one unit per slot, no option93 or absolute expiration. User confirmed the bonus requires all20 slots. Optional category question remains unanswered; current behavior follows the original broad requirement.
- [x] Audit dungeons and reproduce any newly found faults before fixes; test reservation and boss HP arithmetic.
- [x] Review integrated changes and correct significant findings.
- [x] Compile all server sources, run collection/dungeon/shop/save regression tests on classes and candidate JAR.
- [x] Maintenance/save, SQL snapshot, migrate, preserve unique old-JAR backup and SHA256 metadata, synchronize SQL exports, install and restart.
- [x] Verify installed tests, SQL default/capacity, server listener and document remaining Unity/manual gameplay limitations.
