# Capsule Map Travel Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove avoidable client frame stalls during capsule map travel and stop XMAP from retrying capsule selections that the server has rejected.

**Architecture:** Keep existing capsule and map-selection wire messages unchanged. On all four client variants, avoid closing an already hidden panel during XMAP completion and make capsule-loading/selection states terminate on rejection or timeout. On Web clients, remove forced resource cleanup from the map-packet handler and retain cleanup only if profiling shows it is needed, scheduled after loading rather than during the transition. On the server, close the wait dialog on every capsule-selection rejection.

**Tech Stack:** Java server source and standalone regression tests; C# Game1/Game2 client source; PowerShell source-extraction harnesses; Unity Profiler for device frame-time and memory comparison.

---

## Investigation Summary

- Web map changes trigger three forced cleanup passes per transition in both Game1 and Game2: `mSystem.gcc()` in the `-24` handler, then `Resources.UnloadUnusedAssets()` plus `GC.Collect()` twice while `loadInfoMap()` parses the rest of the map packet. The main client does not have those map-packet cleanup calls.
- Both `Game1` and `Game2` copies of `XmapController.FinishXmap()` call `GameCanvas.panel.hide()` unconditionally. The four client copies have this path. `Panel.hide()` invokes `mSystem.gcc()` even if the panel was already hidden, so reaching or cancelling an XMAP route can cause a forced cleanup on every client variant.
- `UseItem.choseMapCapsule()` rejects a full/restricted destination by sending a notice and returning without `hideWaitDialog()`. Manual selection has already displayed a wait dialog. The XMAP path also receives the notice through `InfoMe.addInfo()` and `AutoXmap.Info()`, but the capacity notice is not currently classified as a reason to stop. `XmapController.Update()` can then request the same map edge again after its 500 ms wait.
- No recent capsule-specific runtime log or device reproduction was found. The forced cleanup and retry paths are confirmed in source; the amount of frame-time impact on each device still needs profiling.

## Files and Responsibilities

- Modify `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/XmapController.cs` and `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapController.cs` to avoid hidden-panel cleanup and bound failed capsule route attempts.
- Modify the `Game1` and `Game2` `AutoXmap.cs` and `XmapData.cs` files in both `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp` and `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp`. Keep all eight files behaviorally aligned.
- Modify `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs` and `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Controller.cs` map-change handlers to stop forced cleanup at packet receipt. Main client map handlers currently have no such call; keep them free of one.
- Modify `SRC/src/nro/models/services_func/UseItem.java` to terminate wait UI on each capsule-selection rejection without changing the existing access restrictions.
- Add a client regression harness at `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/CapsuleMapTravelChecks.ps1`; it accepts the Web client root as an additional root and exercises Game1/Game2 in both trees.
- Add a standalone server regression test at `SRC/test/nro/models/map/CapsuleMapSelectionFailureTest.java` for the full-zone/restricted-map rejection behavior.

## Task 1: Capture client regressions across all four variants

**Files:**
- Create: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/CapsuleMapTravelChecks.ps1`
- Read: the `Game1` and `Game2` copies of `XmapController.cs`, `AutoXmap.cs`, and `XmapData.cs` under both `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp` and `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp`, plus the `Game1/Controller.cs` and `Game2/Controller.cs` map handlers in both roots.

- [ ] Extract the actual `FinishXmap()`, `AutoXmap.Info()`, the capsule wait section in `XmapData.Update()`, the route-step section in `XmapController.Update()`, and the Web `case -24` plus `loadInfoMap()` blocks into small C# harnesses, following `Tests/InstantTransmissionFreezeChecks.ps1`.
- [ ] Assert that completing/cancelling XMAP with a hidden panel causes zero panel hides/cleanup calls, while a visible panel closes once.
- [ ] Assert that a rejected capsule selection stops XMAP and that repeated ticks do not send another `requestMapSelect`.
- [ ] Assert that a missing capsule-map response expires the loading latch, and that a successful response still permits the route to continue.
- [ ] Assert that Game1/Game2 and main/Web variants preserve these behaviors; assert the main client has no per-map forced cleanup and Web does not run cleanup from map-message parsing after Task 5.
- [ ] Run from PowerShell with the Web root passed as an additional client root:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\Admin\Downloads\PRJ_2Tab_550K\Tests\CapsuleMapTravelChecks.ps1 -AdditionalClientRoot C:\Users\Admin\Downloads\nroteam\Teamobi2026\ClientWeb\PRJ_2Tab_Web
```

Expected: all four client variants report PASS and zero repeated map-selection requests after a failure.

## Task 2: Make capsule-map failure a terminal, bounded client state

**Files:**
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/AutoXmap.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/XmapData.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/XmapController.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/AutoXmap.cs`, `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapData.cs`, and `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapController.cs`
- Modify: `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/AutoXmap.cs`, `XmapData.cs`, and `XmapController.cs`
- Modify: `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/AutoXmap.cs`, `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapData.cs`, and `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapController.cs`
- Test: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/CapsuleMapTravelChecks.ps1`

- [ ] Let `XmapData` own the pending capsule map-list request and timeout; let `XmapController` own the pending selected destination and timeout. Record request start time and expected map ID before sending each request.
- [ ] When `AutoXmap.Info()` receives the existing full-zone or capsule-denial notices while a capsule request is pending, stop XMAP, clear the pending request and capsule-loading flags, and display the server notice once. Keep the existing behavior for unrelated notices.
- [ ] Use a 10-second response timeout for the map-list request and for a selected destination that has not changed the map ID. If the expected map-change packet arrives, clear the selection timeout and let the existing map loader finish. Do not retry the same selection every 500 ms while it is pending.
- [ ] On success, clear pending state when the expected map ID arrives, then continue the route exactly once. On failure, require a fresh user start before another route attempt.
- [ ] Preserve the existing capsule-return target, map route, and selected map index behavior. Do not change message IDs or payloads.
- [ ] Run the client regression command from Task 1.

Expected: success advances once; a server rejection or absent response ends the pending operation without repeated packets or a stuck `IsLoadingCapsule` latch.

## Task 3: Close wait UI for every rejected server capsule selection

**Files:**
- Modify: `SRC/src/nro/models/services_func/UseItem.java`
- Test: `SRC/test/nro/models/map/CapsuleMapSelectionFailureTest.java`

- [ ] Add a narrow rejection helper in `UseItem` that sends the existing notice and calls `Service.gI().hideWaitDialog(pl)` exactly once.
- [ ] Route the full-zone and restricted-map rejection branches through that helper. Preserve the current `> 25` capacity threshold and Doanh Trai/Ma Bư/Hủy Diệt restrictions.
- [ ] Keep invalid index and Namek Dragon Ball rejection behavior intact; ensure each rejection path closes the wait dialog once.
- [ ] Add standalone test cases for a full zone and each restricted map class, asserting the player is not moved, no state is committed as a successful capsule travel, and one wait-dismiss response is sent.
- [ ] Compile the changed Java source and run the new standalone test using the existing `SRC` test runner conventions; do not connect to production SQL.

Expected: every rejected manual selection clears the client wait dialog and returns one explanatory notice; successful capsule travel remains unchanged.

## Task 4: Remove redundant forced cleanup from XMAP completion

**Files:**
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/XmapController.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapController.cs`
- Modify: `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/XmapController.cs`
- Modify: `C:/Users/Admin/Downloads/nroteam/Teamobi2026/ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/XmapController.cs`
- Test: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/CapsuleMapTravelChecks.ps1`

- [ ] In `FinishXmap()`, call `GameCanvas.panel.hide()` only when the panel exists and is currently visible. Keep XMAP flags, path reset, and user-visible panel closure behavior intact.
- [ ] Apply the same edit to all four copies and compare their method bodies to prevent drift.
- [ ] Run the Task 1 regression harness and confirm zero forced cleanup calls from finishing with an already hidden panel.

Expected: finishing or cancelling an automated route with no visible panel does not invoke `Panel.hide()` and `mSystem.gcc()`; a visible XMAP panel still closes once.

## Task 5: Remove cleanup work from Web map-packet handling

**Files:**
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/Controller.cs`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/GameScr.cs`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/GameScr.cs`
- Test: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/CapsuleMapTravelChecks.ps1`

- [ ] Remove all three forced cleanup sites from each Web map transition: `mSystem.gcc()` in `case -24`, and the two `Resources.UnloadUnusedAssets()`/`GC.Collect()` pairs in `loadInfoMap()`.
- [ ] Profile repeated capsule travel before adding replacement cleanup. Do not call Unity resource APIs or `GC.Collect()` from message parsing.
- [ ] If profiling shows memory growth that fails the baseline comparison, set one pending cleanup flag after map load and consume it from the existing `GameScr` per-frame update on the Unity main thread. Run at most one asynchronous `Resources.UnloadUnusedAssets()` operation after loading is complete and input/map state has been idle for 2 seconds; never force managed GC on each transition.
- [ ] Run the Task 1 harness and compile the affected client source sets to isolated outputs only.

Expected: Web map changes no longer invoke forced resource cleanup from the network-message handler; map loading still completes and memory remains within the measured baseline budget.

## Task 6: End-to-end verification and handoff

- [ ] Run the client source harness for all four variants and the server capsule rejection test.
- [ ] Profile at least 10 consecutive capsule departures and returns on a representative low-end device, recording frame-time spikes, GC activity, and memory before and after.
- [ ] Check manual capsule selection, XMAP capsule selection, return-to-previous-map, successful selection, full-zone rejection, restricted-map rejection, and no-response timeout.
- [ ] Confirm no protocol payload changed and inspect the final diff for unrelated workspace edits.
- [ ] Report source verification separately from device profiling. Do not build or package JAR/APK/EXE/Web release artifacts or deploy/restart services; workspace policy requires an explicit build request.

## Acceptance Criteria

- No hidden-panel `Panel.hide()` call occurs on XMAP completion in any of the four client variants.
- Web map packet handling does not call forced resource cleanup; any retained cleanup is deferred to the Unity main-thread update, coalesced, and supported by memory profiling.
- A failed or unacknowledged capsule request cannot produce repeated map-selection packets or leave XMAP in a loading state indefinitely.
- Every server rejection clears a manual capsule wait dialog and preserves current admission restrictions.
- Successful capsule departure and return work in Game1/Game2 on both client roots with the existing server protocol.
