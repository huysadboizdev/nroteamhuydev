# Boss Equipment Base Bonus Implementation Plan

> **Execution status:** Implemented and installed as `SRC/20.jar` on 2026-10-17. Build and rollback metadata are in `SRC/jar-backup`.

**Goal:** Make boss-dropped Sealed, Divine, and Sealed Divine equipment gain a 7–15% bonus to their original primary stats and retain one separate HP/KI/attack option of 1–5%.

**Architecture:** Use one shared boss-equipment bonus operation after the item’s original options have been assembled and before the drop is published. It will scale only primary stat options, then add a single secondary percentage option when absent. Apply it once across legacy native drops, requested category drops, and configured boss drops, while leaving non-boss acquisition paths unchanged.

**Tech Stack:** Java game server; existing `Item` and `ItemOption` model.

---

### Current behavior to replace

- `DestroyerBonusRoll` scales primary options `{0,22,23,14,27,28,47}` and adds one option from `{77,103,50}` at 1–5%, but its weighted primary bonus includes 0–6%; it does not meet the requested 7% floor.
- `ItemDefaultOptions.completeSealBonus` gives sealed items one 1–5% secondary option but does not scale primary stats.
- `SealedBossEquipmentLoot` creates requested sealed shop equipment and divine equipment. Its ordinary divine branch starts at canonical 100% stats; its sealed branches currently receive only the seal secondary option.
- Legacy `ItemService.randDoTL` and `randDoTLBoss` independently roll `100–115%` for divine stats, so some can remain below 107%.
- `BossAdminDropService` publishes configured rule options directly; a shared adjustment there is needed for targeted equipment configured by an admin.

### Stat and rounding rules

- Roll an integer primary-stat bonus uniformly from **7 through 15 inclusive** for each boss-dropped equipment item.
- Scale only primary option IDs `{0,22,23,14,27,28,47}` using ceiling arithmetic: `ceil(base * (100 + bonusPercent) / 100)`. This ensures small values, such as option 14 with parameter 14, visibly increase and do not truncate below the requested percentage.
- Add one secondary option chosen uniformly from HP `{77}`, KI `{103}`, or attack `{50}`, with a parameter from **1 through 5 inclusive**, when the item does not already contain one of those options. Preserve an explicitly configured existing secondary option rather than duplicating it.
- Do not scale the secondary option, seal markers `{34,35,36}`, rarity/effect options such as `{207}`, or unrelated options.
- Roll the primary bonus once per item, not independently for every primary option; every primary stat on that item uses the same percentage.

### Task 1: Add a shared boss-drop bonus operation

**Files:**
- Create: `SRC/src/nro/models/shop/BossEquipmentBonusRoll.java`

- [x] Implement a deterministic-random overload and a production overload. Validate the target item and option templates before mutation.
- [x] Use one 7–15 roll, ceiling-scale each present primary option ID, preserve all other options, add one 1–5 secondary option only if none of `{77,103,50}` is present, and refresh `item.info` after mutation.
- [x] Keep `DestroyerBonusRoll` unchanged so the separate Beerus shop keeps its current weighted 0–15 behavior.

### Task 2: Apply the shared operation to requested sealed and divine drops

**Files:**
- Modify: `SRC/src/nro/models/services/SealedBossEquipmentLoot.java`

- [x] After building shop or canonical divine options, attach the seal marker and normalize as today, then call the shared bonus operation exactly once for every category, including ordinary unsealed divine gear.
- [x] Remove the boss-loot dependence on `completeSealBonus` so sealed boss equipment does not receive both the old 1–5 option and a second option from the new operation.
- [x] Preserve `completeSealBonus` behavior for other non-boss sealed acquisition paths.

### Task 3: Cover legacy native and configured boss drop paths

**Files:**
- Modify: `SRC/src/nro/models/services/ItemService.java`
- Modify: `SRC/src/nro/models/services/BossAdminDropService.java`

- [x] Route `randDoTL` and `randDoTLBoss` divine equipment through the shared operation, and remove their separate `100–115%` primary-stat scaling so each item is rolled only once.
- [x] Preserve existing rare-effect option behavior and all existing non-stat options in those legacy methods.
- [x] In configured boss drops, construct an item from the rule’s option list, apply the shared operation once to sealed shop equipment in `{230–281}` or any divine equipment in `{555–567}` when its template type is an equipment slot (`0–4`), then publish the completed item. This excludes unsealed shop gear, radars, and other non-equipment entries in the numeric ranges.
- [x] Leave unrelated configured boss drops unchanged.

### Task 4: Source verification and release handoff

**Files:**
- Review only: the four modified Java files above and their existing callers.

- [x] Trace each category through its native, requested, and configured drop path and confirm the shared bonus operation is called once per generated item.
- [x] Confirm the same primary percentage is applied to all primary options on an item, existing explicit secondary options are retained, and non-equipment rewards are unchanged.
- [x] Review the final diff and preserve all pre-existing workspace edits.
- [x] Build the application JAR after the explicit user request, verify its archive and required class entries, verify the main class manifest, back up the prior JAR, and record SHA-256 metadata before installation.

---

## Scope notes

- Target categories: sealed shop equipment (including the Ấn variants identified by seal options), unsealed Divine gear, and sealed Divine gear.
- Existing boss reward drop chances are out of scope; this change affects item stats only.
- This plan does not apply the new roll to player trades, shop purchases, admin grants, saved inventory, or old items already in circulation.
- The current workspace has unrelated/pre-existing source modifications in several target files; implementation must inspect and preserve those edits before changing them.
