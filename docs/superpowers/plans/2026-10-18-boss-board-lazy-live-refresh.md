# Boss Board Lazy Load and Live Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Boss and Boss nhiệm vụ lists load on deliberate selection, show a concise and accurate boss state, and stay fresh by server push without a spinner or periodic polling.

**Architecture:** Keep a client cache per category/view/page and show it immediately. The first explicit request subscribes the server session to that query; tracker changes push a complete filtered page snapshot over command `-58`, which silently replaces the cache. On reconnect, a client that has opened Boss sends one fresh snapshot request. Countdown display uses the server timestamp and only appears for defeated/resting rows with a valid ETA.

**Dependency:** Apply [Hide Selected Boss Notices Everywhere](2026-10-18-hide-selected-boss-notices-everywhere.md) first so normal and pushed snapshots share the approved suppression rule.

**Tech Stack:** Java boss tracker/wire service, C# Unity clients for APK and Web, existing BossNoticeBoard and model regression harnesses.

---

## File map

Server protocol and session:

- Modify `SRC/src/nro/models/services/BossNoticeService.java` to subscribe after an explicit list request and push a filtered snapshot after boss state changes.
- Modify `SRC/src/nro/models/network/MySession.java` to retain the active Boss query and server-push revision for the connected session. Override `dispose()` to unregister before calling `super.dispose()`.
- Modify `SRC/src/nro/models/server/Controller.java` to remove the login-time command that currently fetches the Boss list. The existing `-58` client request dispatch remains the explicit subscribe/request entry point.
- Update existing `SRC/test/nro/models/services/BossNoticeWireTest.java` and `BossNoticeTrackerTest.java` for push packet and subscription lifecycle behavior.

Shared model and APK client:

- Modify `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/BossNoticeData.cs` for short summaries based on `Status`, `Map`, `Next`, `Eta` and server time.
- Modify both `Game1/BossNoticeBoard.cs` and `Game2/BossNoticeBoard.cs`; both copies must use the same cache key, request rules, push revision handling and rendering behavior.
- Modify both `Game1/Panel.cs` and `Game2/Panel.cs` to retain lazy entry behavior and refresh visible rows from the cache without re-entering the Boss request path.
- Modify both `Game1/Service.cs` and `Game2/Service.cs`, `Game1/Controller.cs` and `Game2/Controller.cs`, and `Game1/Session_ME.cs`/`Session_ME2.cs` plus their Game2 copies for the `-58` push/reconnect path.
- Update existing `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/BossNoticeBoardTests.cs` and `BossNoticeModelTests.cs` with cache, push and summary checks.

Web client:

- Apply the same model, board, panel, service, controller and session changes under `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/` for Game1 and Game2.
- Update `ClientWeb/PRJ_2Tab_Web/Tests/BossNoticeBoardTests.cs` and `BossNoticeModelTests.cs` to match APK expectations.

## Task 1: Remove implicit load and define first-click cache behavior

**Files:**
- Modify: `SRC/src/nro/models/server/Controller.java`
- Modify: APK and Web Game1/Game2 `BossNoticeBoard.cs`, `Panel.cs`, `Service.cs`
- Modify: existing `BossNoticeBoardTests.cs` in APK and Web

- [ ] Remove the login-time `requestV2(..., request=0, ...)` / `request(..., request=0, ...)` at the end of player capability setup. On the first deliberate list selection, send the existing BOSS2 `-58` request format directly; its response confirms category support. Do not request rows during login or panel opening.
- [ ] In each `BossNoticeBoard.Select`, keep cached rows while changing category, view or page. Render a matching cached snapshot immediately, then issue one explicit fresh BOSS2 request. For an uncached query, show a short text state such as **Đang cập nhật danh sách Boss** without a spinner. Remove the current early return that blocks the first **Boss nhiệm vụ** request while `categoriesSupported` is still false.
- [ ] Keep `mode < 0` as the top-level/system-notice state. Opening panel type 23 alone must not trigger a list request; selecting a Boss list item must.
- [ ] Add checks to the existing board harnesses that opening the notification panel makes zero requests, selecting Boss makes one request, and reselecting cached Boss renders rows before its fresh response.

## Task 2: Render concise state and truthful countdowns

**Files:**
- Modify: APK and Web `Assets/Scripts/BossNoticeData.cs`
- Modify: APK and Web Game1/Game2 `BossNoticeBoard.cs` and `Panel.cs`
- Modify: existing `BossNoticeModelTests.cs` and `BossNoticeBoardTests.cs`

- [ ] Add one summary formatter to `BossNoticeEntry`. For `ALIVE`, render `Name · Sống · Map` and do not include countdown. For `DEFEATED` or `DEPARTED`/resting with `Next > serverNow`, render a short respawn countdown. For `Eta == "CONDITION"` without a valid `Next`, render **Chờ điều kiện**; for schedule-based ETA, render **Chờ lịch**; otherwise render **Chưa rõ giờ hồi sinh**.
- [ ] Preserve server-time countdown calculation using the received snapshot timestamp and monotonic elapsed time. Never infer an ETA from the boss type, map or defeat time.
- [ ] Change list rows and the selected-row detail heading to show the summary first. Keep only useful secondary details below it, and add a concise `Cập nhật HH:mm:ss` timestamp for the current snapshot.
- [ ] Update the existing model/board checks so alive rows show only the map, valid dead/resting ETA counts down, expired/unknown ETA never becomes a made-up timer, and both client trees format the same strings.

## Task 3: Add per-session server-push snapshots

**Files:**
- Modify: `SRC/src/nro/models/services/BossNoticeService.java`
- Modify: `SRC/src/nro/models/network/MySession.java`
- Modify: `SRC/test/nro/models/services/BossNoticeWireTest.java`
- Modify: `SRC/test/nro/models/services/BossNoticeTrackerTest.java`

- [ ] Extend the current `-58` response protocol with an unsolicited snapshot marker `-3`. Its payload is `category:byte`, `kind:byte`, `page:short`, `pages:short`, `revision:long`, `serverNow:long`, `warning:utf`, `count:short`, then the existing row encoding and category metadata. Keep `-2` request responses and legacy positive request IDs unchanged.
- [ ] After a valid explicit `requestV2`, store the selected category, kind and page on `MySession` and mark it subscribed. Do not subscribe from the removed login-time path.
- [ ] On `appeared`, `defeated`, `departed` and `resting` tracker transitions, increment a monotonic service revision and send the current filtered page snapshot to connected subscribed sessions. Reuse the same filtering, category sorting, pagination and row writer used by normal responses. Coalesce multiple transitions for one session into at most one snapshot per two seconds.
- [ ] Add `BossNoticeService.unsubscribe(MySession)` and call it from `MySession.dispose()` before `super.dispose()`. Ignore disconnected sessions and sessions whose player no longer owns that session.
- [ ] Extend existing wire/tracker checks for marker `-3`, row/category field order, revision ordering, query replacement when the player changes page/category, no subscription before first explicit request, and unsubscribe on disconnect. Run `ant test` from `SRC`; expected: normal `-58` requests still decode and only subscribed sessions receive push snapshots.

## Task 4: Apply server pushes to cached APK data without a spinner

**Files:**
- Modify: APK Game1/Game2 `BossNoticeBoard.cs`, `Controller.cs`, `Service.cs`, `Session_ME.cs`, `Session_ME2.cs`
- Modify: APK Game1/Game2 `Panel.cs`
- Modify: APK `Tests/BossNoticeBoardTests.cs`

- [ ] Teach `BossNoticeBoard.Receive` to recognize marker `-3`, decode the snapshot, verify its category/kind/page and reject revisions older than the cached revision. Store a valid snapshot under its cache key even when the panel is hidden or displaying system notices.
- [ ] Update a visible Boss list from the new snapshot without clearing entries first or showing pending/loading UI. Keep the countdown clock based on the push's `serverNow`.
- [ ] On transport reconnect, send one silent full snapshot request only if the player explicitly opened a Boss list earlier in the current login. Do not poll; preserve cached rows until the response arrives. On a new login/account reset, clear the prior account cache and subscription intent.
- [ ] Extend the existing APK `BossNoticeBoardTests.Run()` and `BossNoticeModelTests.Run()` checks for the new paths, then invoke those methods through the project's current Unity Editor test entry point. Do not call `ClientPasteBuild.Run()` because it builds a standalone EXE; do not build an APK.

## Task 5: Port the same wire and cache behavior to Web

**Files:**
- Modify: Web Game1/Game2 `BossNoticeBoard.cs`, `Controller.cs`, `Service.cs`, `Session_ME.cs`, `Session_ME2.cs`, `Panel.cs`
- Modify: Web `Tests/BossNoticeBoardTests.cs` and `BossNoticeModelTests.cs`

- [ ] Port the APK `-3` decoder, cache-key/revision rules, lazy request behavior, reconnect request, short summary text and no-spinner render behavior without changing packet field order.
- [ ] Verify both Web tabs accept server push while the Boss subsection and whole panel are hidden, preserve the last selected query, and render the new cache immediately on re-entry.
- [ ] Extend the existing Web `BossNoticeBoardTests.Run()` and `BossNoticeModelTests.Run()` checks for the new paths, then invoke those methods through the project's current Unity Editor test entry point. Expected: Game1 and Game2 pass the same cache, stale-revision, reconnect and summary cases as APK; do not run a WebGL build.

## Acceptance checklist

- Opening **Thông báo** alone performs no Boss-list request. The first explicit Boss/Boss nhiệm vụ selection gets a fresh server snapshot.
- After the first selection, the current query is subscribed for the connection. Server state changes update the client cache through push snapshots even while the subsection or panel is closed; there is no periodic polling and no background spinner.
- Re-entry renders the latest available cache immediately and silently reconciles with a fresh snapshot. Reconnect refreshes once only for players who previously opened Boss.
- Alive bosses show their map and no countdown. Dead/resting bosses show a countdown only when the server supplies a valid future ETA; otherwise a short waiting/unknown state appears.
- APK and Web Game1/Game2 use identical cache, revision, packet and rendering rules.
- No APK, EXE, JAR or WebGL artifact is built without a separate explicit build request.
