# Boss Shop Level Drops Implementation Plan

> **For agentic workers:** Execute inline in the current workspace and preserve unrelated working-tree changes.

**Goal:** Give the Nappa assault squad and the requested Fide, Android, Pic/Poc/King Kong, and Nappa bosses a 70% chance to drop one planet-matched level 8–12 shop gear item with a 5–15% increase to its base stats.

**Architecture:** Add a small loot service that reads eligible shop gear from the loaded shop catalog, builds an item from copied shop options, and applies a per-item base-stat multiplier. Call it once from `BossAdminDropService` after native rewards, using an explicit semantic boss-ID allowlist and the existing reward claim guard. Keep existing reward behavior and the existing 7–15% bonus helper unchanged.

**Tech Stack:** Java, existing `Manager.SHOPS` and `ItemService`, `BossAdminDropService`, `ItemMap`.

---

### Task 1: Add the targeted shop gear loot policy

**Files:**
- Create: `SRC/src/nro/models/services/BossShopLevelLoot.java`
- Modify: `SRC/src/nro/models/services/BossAdminDropService.java`

- [x] Implement the eligible semantic IDs: `SO_1`, `SO_2`, `SO_3`, `SO_4`, `TIEU_DOI_TRUONG`, `FIDE`, `ANDROID_19`, `DR_KORE`, `ANDROID_13`, `ANDROID_14`, `ANDROID_15`, `PIC`, `POC`, `KING_KONG`, `KUKU`, `MAP_DAU_DINH`, and `RAMBO`.
- [x] For Fide, allow the new reward only when the final form is defeated, so the three-form sequence gets one new loot roll while native stage rewards stay intact.
- [x] Exclude quest-support copies, bots, missing credited owners, missing boss location/map, and bosses outside the allowlist.
- [x] Build the candidate pool from loaded shop rows. Keep only unique item templates with gear type 0–4, level 8–12, matching the credited player's gender (or common gender greater than 2), nonempty shop options, and enabled status. Sort candidates by level then item ID so selection is deterministic for a given random stream.
- [x] Make one 7000/10000 roll per claimed boss reward. On success, select one level uniformly from levels that have candidates, then select one candidate uniformly from that level. This yields at most one new gear item per boss reward and prevents levels with more shop rows from being overrepresented.
- [x] Create the item through `ItemService.createItemFromItemShop` so shop options are copied; apply one random integer bonus from 5 through 15 inclusive to base-stat option IDs `0, 14, 22, 23, 27, 28, 47`, rounding upward with overflow-safe arithmetic. Preserve other shop options and do not append secondary bonus options or seals.
- [x] Publish the item as a private `ItemMap` owned by the credited player at the boss location.
- [x] Invoke the new policy once from `BossAdminDropService.rewardClaimed` only when the admin policy is not in `replace` mode; leave native callbacks and `RequestedBossEquipment` behavior intact.

### Task 2: Inspect the exact diff and static invariants

**Files:**
- Review: `SRC/src/nro/models/services/BossShopLevelLoot.java`
- Review: `SRC/src/nro/models/services/BossAdminDropService.java`

- [x] Confirm the allowlist contains exactly the approved bosses and maps Android 20 to `DR_KORE`.
- [x] Confirm every eligible target produces at most one new item per reward claim and uses 70% as the single success roll.
- [x] Confirm item templates are from loaded shop rows and strictly within levels 8–12, gear types 0–4, and the credited player's gender or common-gear gender.
- [x] Confirm base-stat scaling uses an inclusive 5–15% bonus, preserves all non-base shop options, and saturates instead of overflowing integer params.
- [x] Confirm no native reward or previous equipment policy was altered; no tests, builds, or JAR operations were run.
