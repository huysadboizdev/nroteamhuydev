# Divine Milestone Base Stat Bonus Implementation Plan

> **For agentic workers:** Implement this plan task by task and preserve the existing uncommitted workspace changes.

**Goal:** Give only each 50-pair accumulation reward a uniformly rolled 1–15% increase to its existing base-stat option values, matching the base-stat calculation used for purchased Destroyer equipment without adding a bonus option.

**Architecture:** Keep reward creation and scaling in `NangSKHVipTichLuy.prepareRewards`, where the transaction snapshot is already prepared. Roll one percentage per reward, scale only the base option IDs used by `DestroyerBonusRoll`, and refresh the item description before it is added to the prepared bag.

**Tech Stack:** Java 17, existing `Item`/`ItemOption` model, `IntUnaryOperator` random source.

---

### Task 1: Scale the accumulated Divine reward's existing base stats

**Files:**
- Modify: `SRC/src/nro/models/combine/NangSKHVipTichLuy.java`
- Reference: `SRC/src/nro/models/shop/DestroyerBonusRoll.java`

- [x] Roll an integer in the inclusive range 1–15 once for each newly created milestone reward.
- [x] Apply the Destroyer base-stat formula `param + param * percent / 100` to existing options with IDs `0, 14, 22, 23, 27, 28, 47`.
- [x] Do not append any new option. Preserve activation set options, item lock, template, type, and planet.
- [x] Recalculate `item.info` after the base values change, then stage the reward as before.

### Task 2: Review the source diff

**Files:**
- Review: `SRC/src/nro/models/combine/NangSKHVipTichLuy.java`

- [x] Confirm the bonus path only runs when creating a queued 50-pair reward, not for normal successful upgrades or later claims of already-created items.
- [x] Confirm one roll is consumed per reward, the endpoints 1 and 15 are reachable, and no option is added.
- [x] Confirm the prepared-bag/transaction flow still owns the changed item instance and persists its updated option values.

