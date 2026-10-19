# Chat `/ak` Support for All Attack Skills — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make chat command `/ak` repeatedly use the currently selected combat skill across supported attack skill types, including charge and special attacks.

**Architecture:** Keep `/ak` as the existing toggle and keep its behavior scoped to the normal auto-attack loop. Dispatch the selected skill through the matching existing client path: targeted attack, self-use attack, or charge lifecycle. Preserve the existing skill selection, cooldown, target and movement rules, and avoid competing with Tàn sát.

**Tech Stack:** Unity C#, existing Game1/Game2 client sources, Python source-extraction regression harness, PowerShell/C# regression helpers.

---

## Agreed behavior

- `/ak` continues to toggle only `autoAttack`.
- AK uses the skill currently selected in the skill slot. It does not rotate through learned skills or change the selected slot on its own.
- Include combat attack skills across template types, including charge skills 10/11 and targeted special skills. Include offensive self-use skills when their existing client path supports them.
- Skip buff/support skills and the revive skill (ID 7). If the user deliberately selects an offensive self-destruct skill, AK follows the selected skill as requested.
- Keep AK suspended while Tàn sát owns combat (`PickMob.tanSat` or `PickMob.tsPlayer`) so both loops cannot attack concurrently.
- Use skill-aware horizontal and vertical range checks. For ordinary mobs, preserve the server-authoritative `xFirst/yFirst` coordinates already used by the combat logic.
- Preserve current map, death, stun/stone, cooldown, mana, target-validity, and connection guards. Only record a skill as used after the corresponding action starts or attack packet is accepted by the local send path.
- Do not change server code, Tàn sát behavior, UI, or the `/ak` command text. Do not build APK, EXE, WebGL, or JAR as part of implementation unless separately requested.

## Source map

Update and keep equivalent behavior in the four current `ModFunc.cs` copies:

- `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/ModFunc.cs`
- `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/ModFunc.cs`
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/ModFunc.cs`
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game2/ModFunc.cs`

Use the matching files under each source root as references; change them only if regression work demonstrates a missing shared behavior:

- Android root `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/`: `Game1/GameScr.cs`, `Game2/GameScr.cs`, `Game1/Char.cs`, `Game2/Char.cs`, `Game1/Service.cs`, `Game2/Service.cs`, `Game1/Mod/XMAP/PickMobController.cs`, and `Game2/Mod/XMAP/PickMobController.cs`.
- Web root `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/`: the same eight Game1/Game2 paths.
- `GameScr.cs` contains `TrySelectSkillForAutoCombat`, `isAttack`, and `doFire`; `Char.cs` contains `isUseChargeSkill`, `sendUseChargeSkill`, `useSkillNotFocus`, and the skill animation/cooldown lifecycle; `Service.cs` contains `TrySendPlayerAttack` and skill selection.
- `PickMobController.cs` provides an existing type-aware dispatch and range policy for comparison only; do not modify Tàn sát.

## Task 1: Add failing AK dispatch regressions

**Files:**

- Create `ClientWeb/PRJ_2Tab_Web/Tests/AutoAttackSkillChecks.py`.
- Read source from the Web project and Android project roots, matching the existing `MassacreSkillChecks.py` pattern.

- [ ] **Step 1: Extract the real AK methods from each source copy.**

  First inventory `SkillTemplate.type` and the special skill IDs used by these sources so the test matrix distinguishes targeted attacks, offensive self-use, charge, special, buff/support, and revive skills. Then extract the chat `ak` case, `AutoAttack()`, and the new AK dispatch/range helpers from each Game1/Game2 `ModFunc.cs`. Keep the harness stubs small and limited to globals those extracted methods call.

- [ ] **Step 2: Assert current selected-skill behavior and unsupported cases.**

  Cover: `/ak` toggles on/off; changing `myskill` while enabled changes the skill used; AK does not rotate `vSkill`; type-1 targeted attacks and type-4 special attacks reach the focused mob/player; offensive type-3 self-use skills use `useSkillNotFocus`; type-2 buffs and revive ID 7 are skipped.

- [ ] **Step 3: Assert charge lifecycle and safety guards.**

  Verify IDs 10 and 11 enter the existing charge path once, do not send a direct attack packet while charging, and do not restart while charge/release is active. Verify no sends while dead, loading a map, stone/stunned, targetless for a targeted skill, disconnected, out of mana, on cooldown, or while Tàn sát owns combat.

- [ ] **Step 4: Assert skill-aware range and pacing.**

  Cover targets inside horizontal and vertical range, targets outside vertical range, and target approach behavior. Verify a ranged skill is not forced into melee range; a melee skill approaches only as needed. Verify one accepted action produces one cooldown update and no repeated sends before it is ready.

- [ ] **Step 5: Run the new regression before implementation.**

  Run from `ClientWeb/PRJ_2Tab_Web`:

  ```powershell
  python Tests/AutoAttackSkillChecks.py
  ```

  Expected before the implementation: failures for the unsupported skill types/charge flow and for any missing range or dispatch behavior. The test must exercise extracted production methods, not a second implementation of the dispatcher.

## Task 2: Implement type-aware dispatch for `/ak`

**Files:**

- Modify the four `ModFunc.cs` files listed in the source map.
- Modify `GameScr.cs`, `Char.cs`, or `Service.cs` only if a required existing action cannot be called safely from `ModFunc`.

- [ ] **Step 1: Validate the selected skill before reading its fields.**

  Return without action when `Char.myCharz()`, `myskill`, or `myskill.template` is null. Keep the existing `/ak` toggle, Tàn sát ownership guard, map/death/control guards, and `paintCanNotUseSkill`/cooldown behavior. Explicitly reject buff/support skills and revive ID 7.

- [ ] **Step 2: Dispatch by the selected skill's existing semantics.**

  - For charge IDs 10/11, use `Char.sendUseChargeSkill()` and leave charging, completion, cancellation, and final cooldown to the existing `Char` lifecycle.
  - For offensive type-3 self-use skills, call `Char.useSkillNotFocus()` without requiring a target.
  - For targeted attack and special skills, retain the focused target and send through `Service.TrySendPlayerAttack` using the existing mob/player packet form.
  - Rely on the existing skill-slot selection packet when the user changes the selected skill. Do not resend skill selection on every AK tick. Use `TrySelectSkillForAutoCombat` only if a tested case shows the AK path must explicitly synchronize a change.

- [ ] **Step 3: Apply range and approach rules before sending.**

  Use the selected skill's range rather than a shared punch threshold. Check both target axes. Preserve `xFirst/yFirst` for ordinary mobs where that is the server's target position. When a target is outside the selected skill's range, approach to that skill's cast spacing without changing the selected skill or sending an out-of-range attack packet.

- [ ] **Step 4: Tie cooldown state to successful action start.**

  Do not call `SetUsedSkill` after a failed send or invalid target. For direct attack packets, update the cooldown only when `TrySendPlayerAttack` reports accepted send. For charge and self-use actions, use the existing lifecycle's start/completion signals so AK cannot duplicate an in-progress cast.

- [ ] **Step 5: Keep Game1/Game2 and Android/Web behavior aligned.**

  Apply the same dispatcher semantics in all four `ModFunc.cs` copies. Keep any platform-specific connection wrappers inside the existing `Service`/`GameScr` methods rather than duplicating protocol logic in AK.

## Task 3: Verify regressions and source parity

**Files:**

- `ClientWeb/PRJ_2Tab_Web/Tests/AutoAttackSkillChecks.py`
- The four `ModFunc.cs` files and any narrowly required shared helper files.

- [ ] **Step 1: Run focused AK regressions.**

  ```powershell
  python Tests/AutoAttackSkillChecks.py
  ```

  Expected: PASS for both games in Android and Web source roots.

- [ ] **Step 2: Run adjacent skill-selection regression.**

  ```powershell
  python Tests/MassacreSkillChecks.py
  ```

  Expected: PASS; this guards the existing type-aware dispatch reference and confirms Tàn sát behavior remains intact.

- [ ] **Step 3: Check the planned source diff.**

  Run `git diff --check` in each changed client repository and compare the four AK implementations for semantic parity. Review that no server source, build output, JAR, APK, EXE, or WebGL artifact changed.

## Plan self-review

- Scope is limited to chat `/ak` and the currently selected skill; Tàn sát is only an ownership guard/reference.
- Skill categories, range, charge lifecycle, cooldown/send result, switching the selected skill, and all four source variants have explicit coverage.
- Build/package work is excluded until the user asks for it.
