# Capsule kì bí tương lai Implementation Plan

> **For agentic workers:** Inline execution authorized by the user. Follow the steps in order.

**Goal:** Unlock the Future Bulma detector shop after the Fide quest, make the active detector give a 70% capsule drop chance from future mobs, and make capsule 380 grant one of ten equally likely buffs.

**Architecture:** Keep existing item IDs and detector timer. Gate the existing `BUNMA_FUTURE` shop on completion of the Fide chapter, update the existing future-mob drop condition, and add a focused capsule-opening method to `UseItem`.

**Tech Stack:** Java game server and MySQL seed data.

---

### Task 1: Gate the Future Bulma shop and list the detector

**Files:**
- Modify: `SRC/src/nro/models/npc_list/BulmaTuongLai.java`

- [x] Use `ConstTask.TASK_22_0`, the start of the next chapter, as the access gate for `BUNMA_FUTURE`.
- [x] Keep item 379 in the existing Future Bulma shop at its configured price.

### Task 2: Update drop and capsule rewards

**Files:**
- Modify: `SRC/src/nro/models/mob/Mob.java`
- Modify: `SRC/src/nro/models/services_func/UseItem.java`

- [x] Change the detector drop chance from 20% to 70% for the existing future mob range.
- [x] Open item 380 into one equally likely item from IDs `{381,382,384,385,1150,1152,1153,1154,383,1151}`; consume one capsule only after reward delivery succeeds.

### Task 3: Verify source changes

- [x] Review the diff and confirm pre-existing workspace edits remain untouched.
- [x] Do not build or package a JAR/APK; the user did not request a build.
