# Web Combat Acknowledgement Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring ClientWeb's mob attack acknowledgement, retry, and auto-attack behavior in Game1/Game2 into parity with the current native client.

**Architecture:** Port the shared combat result tracker and diagnostics, then merge the native combat-result flow into Web Game1/Game2 while retaining WebSocket transport, browser storage, and other Web platform adapters. Keep both account lanes isolated with lane IDs 1 and 2.

**Tech Stack:** Unity C#, Python source regression checks, PowerShell Add-Type harnesses.

---

### Task 1: Add a regression for the missing Web combat result flow

**Files:**
- Create: `ClientWeb/PRJ_2Tab_Web/Tests/CombatAttackParityChecks.py`
- Test: native and Web shared tracker, Service callback path, result packet, manual retry, auto-attack, and massacre dispatch for Game1/Game2.

- [x] Assert the existing Web source has each required combat-result behavior and run the check to capture the expected failures.

### Task 2: Port the shared result tracker and integrate both Web game lanes

**Files:**
- Create: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/MobAttackResultTracker.cs` and `.meta`
- Create: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/MobAttackTrace.cs` and `.meta`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/{Char,Controller,ModFunc,Service}.cs`
- Modify: `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/PickMobController.cs`
- Modify: the same five paths under `Game2`

- [x] Preserve WebSocket send/receive, session selection, and Unity WebGL-specific return checks while adding native-compatible sequence tracking and callbacks.
- [x] Keep lane 1 and lane 2 state independent and clear pending state on disconnect.
- [x] Do not copy the unrelated `ThreadSafeMessageQueue` helper or overwrite platform-specific code.

### Task 3: Verify the parity and existing focused regressions

**Files:**
- Test: `ClientWeb/PRJ_2Tab_Web/Tests/CombatAttackParityChecks.py`
- Test: `ClientWeb/PRJ_2Tab_Web/Tests/AutoAttackSkillChecks.py`
- Test: `ClientWeb/PRJ_2Tab_Web/Tests/MobFrameRecoveryChecks.py`
- Test: `ClientWeb/PRJ_2Tab_Web/Tests/EffectDataChecks.py`

- [x] Run all four checks and inspect the final diff; do not build WebGL/APK/EXE/JAR.
