# Hide Selected Boss Notices Everywhere Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Suppress every notice for Mabư 12h, Mabư 14h, Sói Hecquyn, Ăn Trộm, Mặt Trời and Ở Dơ on the server, APK and Web, including old rows displayed in the Boss board.

**Architecture:** Add a notice-only predicate based on boss identity, class and map context; keep it separate from loot/equipment eligibility. Apply it before tracker writes and broadcasts, filter stored rows before categorizing/paging, and retain client-side filtering for typed overlay events as a fallback.

**Tech Stack:** Java server (`SRC`), Unity C# client source in the APK and Web repositories, existing Java/Python/C# regression harnesses.

---

## File map

Server policy and lifecycle:

- Modify `SRC/src/nro/models/services/BossAnnouncementPolicy.java` with a notice-only predicate and matching history-row predicate. Keep `excluded(Boss)` and `equipment(Boss)` behavior scoped to drop eligibility.
- Modify `SRC/src/nro/models/boss/Boss.java` so `noticePublic()` and `noticeLookup()` both reject the selected bosses before announcements or tracker use.
- Modify `SRC/src/nro/models/services/BossNoticeService.java` so appeared, defeated, departed, resting and row-query paths all honor the same suppression rule before writes or pagination.
- Guard direct kill broadcasts in `SRC/src/nro/models/boss/MajinBuu_12h/{BuiBui,BuiBui2,Cadic,Drabura,Drabura2,Drabura3,Goku,Yacon}.java` and `SRC/src/nro/models/boss/MajinBuu_14h/{Mabu2H,SuperBu}.java`.
- Update existing checks in `SRC/test/nro/models/services/BossAnnouncementTest.java`, `BossNoticePolicyTest.java`, `BossScreenNoticePolicyTest.java` and `BossNoticeWireTest.java`.

APK client:

- Update `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/BossScreenNoticeData.cs` for the selected semantic identities, keeping existing dungeon filtering.
- Review and, where needed, update the paired ticker/overlay code in both `Game1` and `Game2`: `Assets/src/f/Controller2.cs`, `GameScr.cs`, `Mod/ShowBoss.cs`, and `ModFunc.cs` under `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/`.
- Update `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/BossScreenNotificationChecks.py` and existing `BossNoticeBoardTests.cs` only for assertions affected by the new all-surfaces rule.

Web client:

- Apply the same `BossScreenNoticeData.cs` and paired `Game1`/`Game2` overlay changes under `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/`.
- Update `ClientWeb/PRJ_2Tab_Web/Tests/BossScreenNotificationChecks.py` and existing `BossNoticeBoardTests.cs` for parity with APK.

## Task 1: Define notice suppression without changing loot rules

**Files:**
- Modify: `SRC/src/nro/models/services/BossAnnouncementPolicy.java`
- Modify: `SRC/src/nro/models/boss/Boss.java`
- Modify: `SRC/test/nro/models/services/BossAnnouncementTest.java`
- Modify: `SRC/test/nro/models/services/BossNoticePolicyTest.java`

- [ ] Add cases proving that all nine Mabư 12h semantic IDs, Mabư/SuperBu on `MAP_MABU_14H`, and the four special boss classes/IDs are notice-suppressed at all locations. Keep negative cases for ordinary bosses. Add explicit checks that `equipment(Boss)` results and reward/drop behavior for a representative boss remain unchanged.
- [ ] Add `suppressedNotice(Boss)` and `suppressedNoticeId(int semanticId, int mapType)` in `BossAnnouncementPolicy`. Include the exact 12h IDs; special classes plus `AN_TROM`, `O_DO`, `O_DO1`, `MAT_TROI`, and `SOI_HEC_QUYN1`; and the Mabu/SuperBu plus `MAP_MABU_14H` condition. Do not make the drop predicate `excluded(Boss)` mean “notice suppressed.”
- [ ] Change `Boss.noticePublic()` and `Boss.noticeLookup()` to reject `suppressedNotice(this)` before their current scope and lookup-special checks. This prevents special lookup exceptions from re-admitting the selected bosses.
- [ ] Run the focused existing server suite from `SRC` with `ant test`. Expected: notice suppression cases pass and equipment/drop-policy cases retain their previous results.

## Task 2: Stop new tracker rows and direct broadcasts

**Files:**
- Modify: `SRC/src/nro/models/services/BossNoticeService.java`
- Modify: eight Mabư 12h files and two Mabư 14h files listed in the file map
- Modify: `SRC/test/nro/models/services/BossNoticePolicyTest.java`
- Modify: `SRC/test/nro/models/services/BossNoticeWireTest.java`

- [ ] Add lifecycle cases for selected bosses entering appeared, defeated, departed and resting states; assert that none creates a new tracker row or sends command 93/115.
- [ ] Guard all four `BossNoticeService` lifecycle methods before touching the tracker. Keep the existing behavior for unselected bosses.
- [ ] Guard each of the ten direct `ServerNotify.gI().notify(...)` kill call sites with the notice predicate. Leave the surrounding rewards, drops, defeat state and quest behavior intact.
- [ ] Run `ant test` from `SRC`. Expected: selected bosses create no new board/broadcast output; ordinary notice lifecycle and boss reward checks remain unchanged.

## Task 3: Hide existing history before every board page is built

**Files:**
- Modify: `SRC/src/nro/models/services/BossAnnouncementPolicy.java`
- Modify: `SRC/src/nro/models/services/BossNoticeService.java`
- Modify: `SRC/test/nro/models/services/BossNoticePolicyTest.java`
- Modify: `SRC/test/nro/models/services/BossNoticeWireTest.java`

- [ ] Extend history predicate cases to cover selected semantic IDs, names used by legacy rows, and both `MABU`/`SUPERBU` history identities. Keep unrelated boss rows visible.
- [ ] Make the history-row predicate reject all selected bosses. In `BossNoticeService.rows(kind)`, filter after merging tracker and SQL data but before sorting, category filtering, and page-count calculation. Preserve all existing records in SQL/outbox.
- [ ] Verify `Boss nhiệm vụ`, `Boss`, current, history and detail responses cannot retrieve a selected row, including when a matching old row is beyond page zero before filtering.
- [ ] Run `ant test` from `SRC`. Expected: no hidden row appears in any kind/category/page and normal pagination counts are unchanged for allowed rows.

## Task 4: Keep APK and Web overlays from exposing selected notices

**Files:**
- Modify the existing `BossScreenNoticeData.cs` in APK and Web
- Modify the existing Game1/Game2 `Controller2.cs`, `GameScr.cs`, `Mod/ShowBoss.cs` and `ModFunc.cs` only where the current typed event path does not already apply the predicate consistently
- Modify the existing `BossScreenNotificationChecks.py` in both client repositories

- [ ] Extend the current typed identity predicate so the nine Mabư 12h IDs are hidden independent of map, the four special identities remain hidden, and Mabư/SuperBu are hidden on `MAP_MABU_14H`. Preserve the current dungeon screen filter.
- [ ] Confirm the same predicate controls drawing and touch hit-testing in Game1 and Game2. Keep the server ticker text unchanged for bosses that remain visible; hidden events must not flash while command 93 and command 115 arrive separately.
- [ ] Run `python Tests/BossScreenNotificationChecks.py` from the APK project and `python ClientWeb/PRJ_2Tab_Web/Tests/BossScreenNotificationChecks.py` from the Teamobi2026 repository. Expected: both report `PASS` for Game1/Game2 identity, visibility and touch-target checks.
- [ ] Review `git diff --check` in each changed repository. Do not build an APK, EXE, JAR or WebGL artifact.

## Acceptance checklist

- No new command-93 ticker, command-115 overlay event, tracker row or outbox row is produced for the selected bosses.
- Old SQL/outbox rows remain stored but are absent from every current/history/category/page/detail response.
- Selected Mabư 12h bosses are suppressed by semantic identity everywhere; Mabư/SuperBu suppression is limited to the 14h map type; the four named special bosses are suppressed by class and identity.
- Bosses outside the selected set retain their current notices, reward behavior and loot eligibility.
- APK and Web Game1/Game2 hide typed overlay events consistently and leave no invisible touch targets.
- No APK, EXE, JAR or WebGL build is made without a separate explicit build request.
