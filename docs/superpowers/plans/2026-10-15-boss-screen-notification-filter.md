# Boss Screen Notification Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide only the requested boss events from the upper-right screen panel on Android/APK and Web/PC while preserving VIP ticker messages and player boss lookup.

**Architecture:** The server classifies boss events using semantic boss identity and map/dungeon context, then sends additive structured metadata for the screen panel while keeping the existing text announcement intact. Both clients use one presentation predicate for painting and hit testing; the lookup board remains independent and continues to include the named special bosses.

**Tech Stack:** Java server and existing boss-notice tests; Unity C# client sources for Game1/Game2; Python/C# client source-check scripts.

---

## File map

Server files:

- Create `SRC/src/nro/models/services/BossScreenNoticePolicy.java` for the pure suppression rule.
- Modify `SRC/src/nro/models/services/BossNoticeService.java` to classify each appearing boss and produce the structured screen event.
- Modify `SRC/src/nro/models/server/ServerNotify.java` to deliver the additional event without changing the current command-93 ticker message.
- Modify the server's message dispatch only if the existing server protocol requires a new unused command registration; select the command after checking all current command handlers.
- Modify `SRC/src/nro/models/services/BossAnnouncementPolicy.java` only for lookup-history visibility of Ăn Trộm, Ở Dơ, Sói, and Mặt Trời. Keep equipment/drop and announcement behavior separate.
- Create `SRC/test/nro/models/services/BossScreenNoticePolicyTest.java`; extend `SRC/test/nro/models/services/BossNoticePolicyTest.java` for lookup-history behavior and add a wire test beside `BossNoticeWireTest.java`.

Android/APK client files:

- Create `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/BossScreenNoticeData.cs` for the typed event and client predicate shared by both tabs.
- Modify `.../Game1/Assets/src/f/Controller2.cs` and `.../Game2/Assets/src/f/Controller2.cs` to parse the additive event.
- Modify `.../Game1/GameScr.cs` and `.../Game2/GameScr.cs` so `chatVip` continues adding the unchanged ticker text while structured boss events feed the overlay.
- Modify `.../Game1/Mod/ShowBoss.cs` and `.../Game2/Mod/ShowBoss.cs` to retain the typed identity and expose a single visibility decision.
- Modify `.../Game1/ModFunc.cs`, `.../Game2/ModFunc.cs`, and both `GameScr.cs` touch handlers so drawing and hit testing skip the same hidden entries.
- Create `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/BossScreenNotificationChecks.py` using the existing temporary C# compilation pattern in the client tests.

Web/PC client files:

- Create `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/BossScreenNoticeData.cs` with the same wire contract and pure client predicate.
- Modify the matching `Game1` and `Game2` `Assets/src/f/Controller2.cs`, `GameScr.cs`, `Mod/ShowBoss.cs`, and `ModFunc.cs` files under `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/`.
- Create `ClientWeb/PRJ_2Tab_Web/Tests/BossScreenNotificationChecks.py` with the same cases as the APK test script.

Do not modify `BossNoticeBoard.cs`, `Panel.cs`, or the lookup wire format unless tests show that the four named special bosses are still filtered from lookup history. The display filter must not be implemented by changing `notifBoss`, because that setting controls all screen notices.

## Task 1: Pin down server boss and location identities

**Files:**
- Read: `SRC/src/nro/models/boss/BossID.java`
- Read: `SRC/src/nro/models/consts/ConstMap.java`
- Read: `SRC/src/nro/models/map/Map.java`
- Read: `SRC/src/nro/models/boss/Boss_Manager/BossManager.java`
- Read: `SRC/src/nro/models/map/phoban/`
- Create: `SRC/test/nro/models/services/BossScreenNoticePolicyTest.java`

- [ ] **Step 1: Add failing policy cases** for the configured Oshin 12-hour boss location, Mabu 14-hour event maps, every dungeon map type, and IDs for Ăn Trộm, Ở Dơ, Sói, and Mặt Trời. Include negative cases for the same 12-hour identities outside Oshin and ordinary non-dungeon bosses. The code currently represents the Mabu 14-hour encounter with `BossID.MABU`/`BossID.SUPERBU` on maps 127/128 (`MAP_MABU_14H`), not a separate `MABU_14H` boss ID.

```java
assertTrue(BossScreenNoticePolicy.hide(id12h, oshinMapId, normalMapType));
assertFalse(BossScreenNoticePolicy.hide(id12h, anotherMapId, normalMapType));
assertTrue(BossScreenNoticePolicy.hide(mabu14hId, oshinMapId, mabu14hMapType));
assertTrue(BossScreenNoticePolicy.hide(unrelatedBossId, dungeonMapId, doanhTraiMapType));
assertTrue(BossScreenNoticePolicy.hide(BossID.AN_TROM, normalMapId, normalMapType));
assertTrue(BossScreenNoticePolicy.hide(BossID.O_DO, normalMapId, normalMapType));
assertTrue(BossScreenNoticePolicy.hide(BossID.SOI_HEC_QUYN1, normalMapId, normalMapType));
assertTrue(BossScreenNoticePolicy.hide(BossID.MAT_TROI, normalMapId, normalMapType));
assertFalse(BossScreenNoticePolicy.hide(unrelatedBossId, normalMapId, normalMapType));
```

- [ ] **Step 2: Run the focused server test and confirm it fails** because `BossScreenNoticePolicy` does not exist yet.

Run from `SRC`: `ant test`

Expected: the focused test reports the missing policy or failing selected cases; existing unrelated boss-notice tests remain unchanged.

- [ ] **Step 3: Implement the pure policy** with an explicit set of semantic 12-hour boss IDs, the configured Oshin map identity, the map-type set used by dungeon implementations, and this Mabu 14-hour condition: semantic ID is `BossID.MABU` or `BossID.SUPERBU` and map type is `ConstMap.MAP_MABU_14H`. Read the Oshin identity and dungeon map types from server constants and map/dungeon definitions; do not use display-name substring matching. Unknown identities and locations return `false` (visible).

```java
public static boolean hide(int semanticBossId, int mapId, int mapType) {
    boolean mabu14h = (semanticBossId == BossID.MABU || semanticBossId == BossID.SUPERBU)
        && mapType == ConstMap.MAP_MABU_14H;
    return SPECIAL_BOSS_IDS.contains(semanticBossId)
        || (TWELVE_HOUR_IDS.contains(semanticBossId) && mapId == OSHIN_MAP_ID)
        || mabu14h
        || DUNGEON_MAP_TYPES.contains(mapType);
}
```

Use the actual 12-hour `BossID` constants and the Oshin map identity from server map configuration. Build `DUNGEON_MAP_TYPES` from existing `ConstMap` constants and dungeon implementations, including Doanh trại and Khí gas. Do not add a made-up Mabu 14-hour boss ID.

- [ ] **Step 4: Run the focused policy test and the existing category/policy tests.**

Run from `SRC`: `ant test`

Expected: selected positive cases are hidden, negative cases stay visible, and existing boss category/drop-policy tests pass.

## Task 2: Send structured screen metadata without changing the ticker

**Files:**
- Modify: `SRC/src/nro/models/services/BossNoticeService.java`
- Modify: `SRC/src/nro/models/server/ServerNotify.java`
- Test: `SRC/test/nro/models/services/BossNoticeWireTest.java`
- Create: `SRC/test/nro/models/services/BossScreenNoticeWireTest.java`

- [ ] **Step 1: Add a failing wire test** for a structured screen event containing a stable event key, semantic boss ID, form, map ID, map type, and the original ticker text. Assert that the existing command-93 text remains exactly `BOSS <name> vừa xuất hiện tại <map>` and that the additive packet does not alter or duplicate it.

```java
assertEquals(originalTickerText, decodedTickerText);
assertEquals(expectedSemanticId, decodedBossEvent.semanticBossId());
assertEquals(expectedMapId, decodedBossEvent.mapId());
assertEquals(expectedMapType, decodedBossEvent.mapType());
assertEquals(expectedEventKey, decodedBossEvent.eventKey());
```

- [ ] **Step 2: Check server/client command tables** and reserve an unused command for the additive event. Confirm unknown commands are safely ignored by older clients; retain command 93 for the ticker.

- [ ] **Step 3: Add a structured boss-event queue/payload** at the server notify boundary. Populate it from `BossNoticeService.appeared(Boss)` using semantic boss identity and map/dungeon context. Send the current ticker text and structured event once each; never place metadata in the ticker string.

```java
ServerNotify.gI().notify(tickerText);
ServerNotify.gI().notifyBossScreen(eventKey, semanticId, form, mapId, mapType);
```

- [ ] **Step 4: Extend the wire test** for unknown schema/version and old text-only messages. Old text remains valid and unknown metadata can be ignored safely.

- [ ] **Step 5: Run server wire/category tests.**

Run from `SRC`: `ant test`

Expected: existing boss lookup command `-58` tests pass; command 93 text is unchanged; the new event decodes to the expected identity/location fields.

## Task 3: Add the shared client filter and Android/APK handling

**Files:**
- Create: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/BossScreenNoticeData.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Assets/src/f/Controller2.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Assets/src/f/Controller2.cs`
- Modify: both `Game1/GameScr.cs`, `Game2/GameScr.cs`, `Game1/Mod/ShowBoss.cs`, `Game2/Mod/ShowBoss.cs`, `Game1/ModFunc.cs`, and `Game2/ModFunc.cs`
- Create: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/BossScreenNotificationChecks.py`

- [ ] **Step 1: Add failing pure C# filter tests** for positive and negative cases, unclassified legacy notices, and unknown IDs. Require unclassified events to remain visible.

```csharp
Check(Filter.ShouldShow(ordinaryBossOutsideDungeon), "ordinary boss remains visible");
Check(!Filter.ShouldShow(oshinTwelveHourBoss), "selected Oshin 12-hour boss is hidden");
Check(!Filter.ShouldShow(doanhTraiBoss), "dungeon boss is hidden");
Check(!Filter.ShouldShow(unknownBoss), "unclassified event fails open");
```

- [ ] **Step 2: Implement the typed payload and pure predicate** in `BossScreenNoticeData.cs`. Match the server wire fields exactly. Keep the helper independent of drawing code so the test can compile it in isolation.

- [ ] **Step 3: Parse the new packet in both Controller2 copies** and route it to the screen overlay. Keep the existing command-93 `chatVip` call responsible for adding the original text to the VIP ticker.

- [ ] **Step 4: Store typed identity in both ShowBoss copies** and make the overlay painter skip entries for which the shared predicate returns false. Apply that same decision in `GameScr.UpdateKeyTouchControl`; do not remove events from the ticker or the boss lookup board.

- [ ] **Step 5: Run the client policy and integration checks** for both Game1 and Game2. The check script compiles the pure helper using the existing `csc.exe` test pattern and source-checks the two dispatcher/draw/hit-test integrations.

Run: `python C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/BossScreenNotificationChecks.py`

Expected: `PASS` for the filter matrix and both tab integrations; no APK build is run.

## Task 4: Port the same behavior to Web/PC

**Files:**
- Create: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/BossScreenNoticeData.cs`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Assets/src/f/Controller2.cs`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Assets/src/f/Controller2.cs`
- Modify matching `Game1` and `Game2` `GameScr.cs`, `Mod/ShowBoss.cs`, and `ModFunc.cs`
- Create: `ClientWeb/PRJ_2Tab_Web/Tests/BossScreenNotificationChecks.py`

- [ ] **Step 1: Port the passing Android helper tests** to the Web/PC test script, adapting only the repo path and C# compiler invocation.

- [ ] **Step 2: Add the same typed decoder and predicate** using the identical field order, identity constants, and fail-open behavior as Android/APK.

- [ ] **Step 3: Wire both Web/PC tabs** so command 93 still feeds the ticker and the structured event drives only the overlay. Use the same filter for drawing and hit testing.

- [ ] **Step 4: Run the Web/PC notification check** and the existing targeted source tests.

Run: `python ClientWeb/PRJ_2Tab_Web/Tests/BossScreenNotificationChecks.py`

Expected: `PASS` for both tabs; ticker text remains byte-for-byte unchanged.

## Task 5: Preserve boss lookup history and verify the full behavior

**Files:**
- Modify: `SRC/src/nro/models/services/BossAnnouncementPolicy.java`
- Modify: `SRC/test/nro/models/services/BossNoticePolicyTest.java`
- Test: `SRC/test/nro/models/services/BossNoticeWireTest.java`
- Read: `SRC/src/nro/models/services/BossNoticeService.java`
- Read: client `BossNoticeBoard.cs` copies and current `BossNoticeBoardTests.cs` in both client projects

- [ ] **Step 1: Add failing lookup-history cases** proving Ăn Trộm, Ở Dơ, Sói, and Mặt Trời remain visible in the player board while the screen filter independently hides them.

```java
for (int id : selectedSpecialBossIds) {
    assertFalse(BossAnnouncementPolicy.excludedHistory(rowFor(id)));
    assertTrue(BossScreenNoticePolicy.hide(id, normalMapId, normalMapType));
}
```

- [ ] **Step 2: Change only history filtering** for the four requested special identities. Preserve any separate announcement, reward, or equipment exclusions and preserve category `QUEST`/`GENERAL` assignments.

- [ ] **Step 3: Run server lookup policy and wire tests**, then run both client notification checks and the existing `BossNoticeBoardTests` harness for each client project.

- [ ] **Step 4: Perform a two-client manual verification matrix** covering: Oshin 12-hour boss; Oshin Mabu 14-hour boss; each dungeon family including Doanh trại and Khí gas; Ăn Trộm; Ở Dơ; Sói; Mặt Trời; an ordinary non-dungeon boss; and a Boss nhiệm vụ event. For each, verify overlay visibility, ticker text, and lookup availability.

- [ ] **Step 5: Run `git diff --check` in each Git repository** and review the final diff to ensure only requested screen notices are filtered and no unrelated workspace changes are staged.

## Acceptance criteria

- The upper-right panel hides only the requested 12-hour/Oshin, Mabu 14-hour/Oshin, dungeon, Ăn Trộm, Ở Dơ, Sói, and Mặt Trời events.
- Unrelated ordinary and quest boss notices remain visible.
- Hidden notices do not leave invisible touch targets.
- The bottom VIP ticker displays the original text unchanged.
- The player lookup continues to include the four named special bosses.
- Both Game1 and Game2 on Android/APK and Web/PC handle the structured event safely; unclassified/unknown metadata remains visible.
- No APK, EXE, JAR, or WebGL artifact is built until the user explicitly requests a build.
