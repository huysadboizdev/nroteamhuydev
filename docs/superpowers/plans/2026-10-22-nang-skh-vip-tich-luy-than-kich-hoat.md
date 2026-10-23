# Nâng SKH VIP Cumulative Activation Crafting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Let players accumulate 50 Divine and 50 Destroyer items over multiple submissions under Ba Hat Mit's Nâng SKH VIP path, then craft one guaranteed random activated Divine item.

**Architecture:** Add a distinct combine mode and service for accumulation, keeping the existing pair-upgrade mode unchanged. Persist two counters and paid-pair count in the existing `data_vip` JSON; submit transactions consume only accepted items and charge one billion gold per newly completed pair. At 50/50, a separate confirmation creates one item using the existing activation item builder, then clears progress.

**Tech Stack:** Java 17 server source, existing combine/NPC menus, `PlayerDAO`/`MrBlue` JSON persistence, `ItemService`, `SKHCapsuleCatalog`.

---

## Files and responsibilities

- Modify `SRC/src/nro/models/player/Player.java`: persistent counters for Divine, Destroyer, and paid pairs.
- Modify `SRC/src/nro/models/database/PlayerDAO.java` and `SRC/src/nro/models/database/MrBlue.java`: append/read the counters in `data_vip` with zero defaults for old saves.
- Create `SRC/src/nro/models/combine/NangSKHVipTichLuy.java`: item validation, selection snapshot, preview, deposit commit, ready prompt, random output, and reset.
- Modify `SRC/src/nro/models/combine/Combine.java`: hold/clear the new confirmation selection.
- Modify `SRC/src/nro/models/combine/CombineService.java`: route the new mode and provide its tab text.
- Modify `SRC/src/nro/models/npc_list/BaHatMit.java` and `SRC/src/nro/models/consts/ConstNpc.java`: nest the new choice under Nâng SKH VIP and handle its deposit/craft confirmations.
- Modify `SRC/src/nro/models/services/ItemService.java`: add a validated Divine activation builder that uses `RewardService.initChiSoItemBase` for the actual Divine base stats and the existing activation option mapping.
- `SRC/src/nro/models/item/SKHCapsuleCatalog.java` provides the planet-specific activation set catalog.

## Task 1: Add persisted accumulation state

- [x] Add `int skhVipDivineProgress`, `int skhVipDestroyerProgress`, and `int skhVipPaidPairs` to `Player.java`, initialized to zero.
- [x] Append those fields after the existing eight entries written into `data_vip` in `PlayerDAO.java`.
- [x] In `MrBlue.java`, after parsing existing indexes 0–7, read indexes 8–10 only when present; otherwise set each new value to zero. Reset invalid progress unless `0 <= paidPairs == min(divineProgress, destroyerProgress) <= 50` and both material counters are at most 50.

## Task 2: Implement safe item selection and cumulative deposits

- [x] Create the selection and state snapshot in `NangSKHVipTichLuy.java`. Classify selected bag items by whitelist: Divine IDs 555–567; Destroyer IDs 650–662. Require quantity 1, no expiry option, permanent lifetime, no activation options, valid template/type, unique slots and unique Item instances.
- [x] Accept only each group's remaining capacity (`50 - currentProgress`); leave any overage selected by the player in the bag. Allow Divine-only, Destroyer-only, or mixed deposits.
- [x] Compute `nextDivine`, `nextDestroyer`, `newPaidPairs = min(nextDivine, nextDestroyer) - skhVipPaidPairs`, and `fee = newPaidPairs * 1_000_000_000L`. Reject the whole preview/confirmation if gold is insufficient for that fee.
- [x] Preview current and resulting counters, accepted item counts, newly completed pairs, and exact gold fee. Snapshot selected slots, object identities, option state, and counters so confirmation can reject stale selections.
- [x] On confirmation, revalidate under the player and inventory locks, then replace accepted item slots with empty items, subtract fee, update counters/paid pairs, clear confirmation state, and sync bags/money. Do not mutate anything on validation failure.

## Task 3: Add guaranteed craft at the threshold

- [x] When both counters equal 50 and `skhVipPaidPairs == 50`, route to a Ba Hat Mit craft confirmation with the ready state; if the inventory has no empty slot, retain progress and explain that space is needed.
- [x] On craft confirmation, recheck readiness and inventory capacity, roll equip type 0–4 and planet 0–2 uniformly, map to Divine template IDs (Earth: 555/556/562/563/561; Namek: 557/558/564/565/561; Xayda: 559/560/566/567/561), then choose uniformly from `SKHCapsuleCatalog.sets(planet)`.
- [x] Build the activated Divine item with a validated `ItemService` helper using `createItemSetKichHoat`, `RewardService.initChiSoItemBase`, and the primary/effect options for the chosen set. Do not use the village-shop option builder for Divine templates. Validate it before mutation; add it to the bag and reset all three counters only after the item is ready to receive. A creation or capacity failure preserves `50/50` and paid progress.
- [x] Make the craft confirmation single-use so repeated menu packets cannot grant duplicate output.

## Task 4: Integrate the new mode under Nâng SKH VIP

- [x] Add combine mode constant `NANG_SKH_VIP_TICH_LUY` without changing `NANG_SKH_VIP` behavior.
- [x] Add a second-level NPC choice under the existing VIP button: current pair upgrade and cumulative Divine activation craft. Route each to its own combine mode while keeping both beneath Nâng SKH VIP.
- [x] Route `showInfoCombine`, `startCombine`, top-tab text, and cancel/reopen handling for the new mode to `NangSKHVipTichLuy`.
- [x] Add the new selection field to `Combine`; clear it from `clearItemCombine`, `setTypeCombine`, and disposal paths.
- [x] Show accumulated progress whenever the new mode opens. If already ready at 50/50, open the craft confirmation instead of asking for more materials.

## Task 5: Review the patch

- [x] Compare all paths against `docs/superpowers/specs/2026-10-22-nang-skh-vip-tich-luy-than-kich-hoat-design.md`: independent counters, fee once per newly completed pair, persisted progress, overflow preservation, guaranteed output, valid random mapping, ready-state retry, and reset only after successful grant.
- [x] Inspect `git diff --check` and the full diff for accidental changes to the existing VIP pair upgrade.
- [x] Do not add/run automated tests or build/package a JAR as part of this request; those require a separate explicit user request under workspace instructions.
