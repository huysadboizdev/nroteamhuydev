# Boss Form and Drop Synchronization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every boss form change display correctly on clients, keep server zone membership stable during same-boss form transitions, and make map-item identifiers safe across the existing 16-bit client protocol.

**Architecture:** Treat the server's `-5` player-info packet as an authoritative refresh for an already-present character. Update the existing client `Char` object in place so character references and active combat state remain valid, while refreshing every field carried by the packet and invalidating appearance caches. Keep boss form progression on its existing server lifecycle unless verification shows a separate server defect. Align item-map ID allocation with the signed-short protocol and guarantee uniqueness among items still present in a zone.

**Tech Stack:** Java server (`SRC/src`), Unity C# client source (`PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp`), existing TCP message protocol, server/client runtime verification.

---

## Confirmed findings and scope

- The server defines multiple forms under one boss identity for Fide, Baby, Cooler, Black Goku, Cumber, Xên Bọ Hung, Siêu Bọ Hung, Bojack, and Mabư 14h.
- For a non-final form, `Boss.leaveMap()` retains the current zone. `respawn()` advances `currentLevel`; the join path targets the same zone. `ChangeMapService.commitToMap()` does not exit the boss when the destination is the current zone.
- `Zone.infoPlayer()` sends the updated boss state in message `-5`. Android Game1 and Game2 parse the packet into a new `Char`, but add it only if that ID is absent. Existing boss sprites therefore retain stale name/outfit/HP data.
- Item-map IDs are allocated as `int`, while drop and pickup messages serialize/read them as signed `short`. Once an ID is outside the signed-short range, the request can no longer identify the same server-side item. This is an item pickup protocol defect; source inspection does not establish that it moves a drop to another zone.
- This plan covers source correction. Per workspace instruction, do not build/package the APK until the user explicitly requests a build. No source change or build is performed while writing this plan.

## Files and responsibilities

- Modify `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs`: refresh an existing character from parsed `-5` state.
- Modify `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Controller.cs`: apply the same behavior to the second game client.
- The corresponding existing character types are `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Char.cs` and `.../Game2/Char.cs`; modify them only to host a focused refresh helper or appearance-cache reset after field inspection.
- Modify `SRC/src/nro/models/map/ItemMap.java`: allocate only IDs representable by the signed-short wire protocol and avoid reuse while an item with that ID remains in the zone.
- Modify `SRC/src/nro/models/map/Zone.java`: expose/use active-ID availability for safe wraparound, if the allocator cannot perform the check through existing APIs.
- Modify `SRC/src/nro/models/services/Service.java` and `SRC/src/nro/models/server/Controller.java` only if wire handling needs explicit range validation; keep both endpoints using the same signed-short interpretation.
- Add focused regression tests under `SRC/test/nro/models/map/` for item ID allocation and server transition behavior. Client behavior is verified with the existing client runtime because this source tree has no confirmed automated Unity test harness.

## Task 1: Preserve the server's same-zone form lifecycle

**Files:**
- Inspect `SRC/src/nro/models/boss/Boss.java:510-710, 822-900`
- Inspect `SRC/src/nro/models/map/service/ChangeMapService.java:870-900`
- Inspect `SRC/src/nro/models/map/Zone.java:530-620`
- No edit unless this verification finds a new defect.

- [ ] Trace the death of one non-final form through `Boss.die()`, `Boss.leaveMap()`, `Boss.respawn()`, `Boss.joinMap()`, `commitToMap()`, and `Zone.infoPlayer()`.
- [ ] Repeat the trace for all nine multi-form boss constructors, checking for overrides of `leaveMap()`, `respawn()`, or `joinMap()` that change the path.
- [ ] Record the expected invariant: a form transition retains the same boss ID and zone, and emits a fresh `-5` character snapshot; it must not require a remove/re-add map transition.
- [ ] If any boss override violates that invariant, add a boss-specific regression case before changing its lifecycle. Do not alter timeout-based `leaveMapNew()` behavior as part of form synchronization.

## Task 2: Refresh existing Android client characters on `-5`

**Files:**
- Modify `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs:4146-4205`
- Modify `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Controller.cs:4142-4201`
- Modify the matching absolute-path `Char.cs` files only if needed for a shared refresh helper or explicit appearance-cache invalidation.

- [ ] Map each value parsed into the temporary `char10` to its packet-owned `Char` field. Include ID, clan/level metadata, PK type, gender, head, name, current/max HP, body, leg, bag/flag, coordinates, aura/effects, mount, and hat as present in the packet.
- [ ] For an unknown ID, preserve the current behavior and add the parsed character to `vCharInMap`.
- [ ] For an existing ID, copy the packet-owned fields into the existing object rather than adding a duplicate. Preserve references held by `charFocus`, combat targets, pets, and other systems; do not overwrite state absent from the packet such as active hit animation, skill execution, or local effect timers.
- [ ] Reset only cached appearance/template data that depends on the changed gender/head/body/leg/aura/hat. Do not reset unrelated attack, death, or movement state.
- [ ] Keep the Game1 and Game2 implementations behaviorally identical; confirm packet readers consume exactly the same bytes in both the existing and new-character cases.

## Task 3: Constrain item-map IDs to the wire format

**Files:**
- Modify `SRC/src/nro/models/map/ItemMap.java:14-90`
- Inspect/modify `SRC/src/nro/models/map/Zone.java:250-285, 430-455`
- Inspect `SRC/src/nro/models/services/Service.java:1613-1645`
- Inspect `SRC/src/nro/models/server/Controller.java:690-700`

- [ ] Define the supported ID range as non-negative signed-short values `0..32767`, matching the existing server writers and reader without changing the packet format.
- [ ] Change allocation to wrap within that range and skip IDs currently assigned to items in the zone. Keep allocation synchronized with item insertion/removal so two simultaneous drops cannot receive the same live ID.
- [ ] If all IDs are occupied, fail the new drop explicitly and log the zone/map and exhaustion; never publish an ID that will be truncated on the wire.
- [ ] Confirm item removal frees the ID for later reuse and that ordinary low IDs retain current behavior.
- [ ] Do not change item ownership, drop coordinates, despawn timing, or boss zone logic in this task.

## Task 4: Verify the boss transition and drop paths

**Files:**
- Read-only runtime verification against `SRC/src` and the APK client source after Tasks 1-3.
- The Web Game1/Game2 source is not present in the four mounted writable project roots; Web implementation and release verification are a separate required platform pass after that client source is mounted.
- Use the server's existing test harness for focused Java regression cases; do not start Unity batchmode or package an APK without explicit build authorization.

- [ ] For each of the nine multi-form bosses, verify a player sees the next form's name/outfit/HP on the same map and zone after the previous form dies.
- [ ] Verify the new form transitions through its chat/non-PK phase and becomes attackable/red without changing the displayed form back to stale data.
- [ ] Repeat with multiple clients in the zone and with clients entering after the transition; check that both receive the same current form.
- [ ] Verify one-form bosses still appear, die, respawn, and leave on their existing schedule; this guards against changing generic `-5` handling incorrectly.
- [ ] Drop items repeatedly in one zone across allocator wraparound. Verify the ID sent in message `68` equals the ID used in the pickup request, each item can be picked once by its eligible owner, and a still-live item cannot be selected by a reused ID.
- [ ] Verify drops remain attached to the zone where they were created and are removed through the existing zone removal path. Record any real cross-zone movement separately from pickup-ID failures.
- [ ] Run the server's focused regression tests. Record exact command and result. Do not claim live APK validation until an APK is explicitly built and exercised.

## Task 5: Release checklist

- [ ] Review the diff for duplicate `Char` IDs, packet-reader drift, stale appearance caches, item ID collisions, and changes to boss timeout/retirement behavior.
- [ ] Build the server and client only when the user authorizes the build under the workspace build rule.
- [ ] If producing an APK or JAR, follow `AGENTS.md`: verify the server is stopped before JAR replacement, back up `SRC/20.jar` with a timestamped unique name, record SHA-256 metadata, and leave only the active application JAR in the run folder.
- [ ] Report separately which checks passed in source tests, server runtime, Android runtime, and Web runtime; do not combine them into one “fixed” claim.

## Acceptance criteria

1. All nine identified multi-form bosses display the current server form after every form transition on both Android game clients.
2. Existing character references remain valid and no duplicate boss sprite is added when message `-5` arrives for an ID already on the map.
3. Same-zone form transitions do not cause server-side zone exit/re-entry or move drops.
4. Every active item-map ID is representable in the existing signed-short protocol and uniquely identifies one live item per zone.
5. Any Web client release is marked complete only after its corresponding source is patched and separately verified; Android results alone do not establish Web behavior.
