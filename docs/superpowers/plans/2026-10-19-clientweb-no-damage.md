# ClientWeb No-Damage Investigation and Recovery Plan

> **Status:** Approved and implemented in source for the confirmed lethal-packet bug. WebGL was not built or deployed.

**Goal:** Make legal manual mob attacks in ClientWeb reliably produce a server-confirmed damage or kill outcome, and report genuine misses/timeouts accurately.

**Architecture:** Keep the game server authoritative for HP and damage. Trace one manual attack across send, server result, normal hit packet (`-9`), and kill packet (`-12`); first distinguish missing telemetry from missing combat outcome. Apply only the smallest evidence-backed fix: correct packet correlation/parsing for false negatives, or add a bounded retry for a confirmed accepted attack with a still-live target and no outcome. Mirror behavior across Game1 and Game2.

**Tech Stack:** Unity C#, existing `MobAttackResultTracker`, ClientWeb diagnostics, focused Python source regression checks. No WebGL build unless the user explicitly requests it.

---

## Evidence and constraints

- The live log observed on 2026-10-19 had 96 `HIT_MATCH` events and 8 `NO_DAMAGE` events at the time checked. Most of the no-damage cases were near a mob's final HP; one session had started before the `-12` kill-packet instrumentation was loaded. Those cases do not prove that the server failed to apply damage.
- `ClientWebCombatDiagnostics` produces `NO_DAMAGE` after a 2-second observation window. That diagnostic does not itself change attack behavior.
- ClientWeb sends normal mob attack command `54`; the server validates range/cooldown and returns result command `-48`. Damage and kill outcomes arrive separately as `-9` and `-12`.
- Confirmed source defect: both ClientWeb `-12` handlers discarded kill packets when `mob.status == 0`, even though this is a live mob state. The mob therefore never received `startDie()` and `RecordMobKill()` was never reached. The duplicated status check was replaced with a guard for already-dying `status == 1` mobs.
- Do not locally subtract mob HP or report a hit without a server packet. Server rejects for range, cooldown, resource, dead/removed target, or other legal conditions must remain authoritative.
- Keep both account lanes isolated and apply identical behavior to Game1 and Game2.

## Files in scope

- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/ClientWebCombatDiagnostics.cs` — outcome classification and correlation trace.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/MobAttackResultTracker.cs` — sequence/result lifecycle and bounded retry state if evidence requires it.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Service.cs` and `Game2/Service.cs` — record the attempted target and sent sequence.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs` and `Game2/Controller.cs` — parse and correlate `-48`, `-9`, and `-12` outcomes.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Char.cs` and `Game2/Char.cs` — manual retry scheduling only if confirmed necessary.
- `ClientWeb/PRJ_2Tab_Web/Tests/CombatNoDamageRecoveryChecks.py` — focused regression checks.

Do not edit server combat rules unless fresh evidence shows the server accepted the exact sequence but did not execute damage. If that occurs, stop and prepare a separate server-side plan because it changes gameplay for all clients.

## Implementation record

- Added `Tests/CombatNoDamageRecoveryChecks.py`; it failed before the fix on both Game1 and Game2 and passed after the fix.
- `CombatDeathTraceChecks.py` passed (1 test); `CombatAttackParityChecks.py` passed (4 tests).
- Current browser log session predates the source fix and cannot verify the new `-12` handling. A new WebGL build and fresh page session are still needed for live confirmation; no build was started under the workspace build rule.

## Task 1: Reproduce and classify a fresh ClientWeb session

**Files:** No production files changed. Read the active `Builds/Logs/combat-*.ndjson` output.

- [ ] Load the currently deployed build in a fresh browser page session; leave Unity Editor and unrelated sessions running.
- [ ] Capture one sequence for each case: ordinary hit on a living mob, hit that kills a low-HP mob, out-of-range attempt, and cooldown attempt.
- [ ] For each sequence, correlate `ATTACK_ATTEMPT`, `SEND`, `RESULT`, `HIT`/`HIT_MATCH`, `MOB_KILL`/`KILL_MATCH`, and target HP/liveness.
- [ ] Classify the failure before editing:
  - `-12` received but no attack matched: kill-event correlation bug.
  - `-9` received with positive damage but no attack matched: hit-event correlation bug.
  - `-9` received with zero damage: server-confirmed zero-damage outcome; inspect server rule and do not retry blindly.
  - `-48` rejected: use server status/retry time; do not count as a landed attack.
  - `-48` accepted, target remains alive, and neither `-9` nor `-12` arrives: candidate for one bounded retry after checking packet/session state.
  - No `-48`: transport/packet path issue; do not infer damage behavior from the animation.

## Task 2: Add failing regression checks for outcome correlation

**Files:** Create `ClientWeb/PRJ_2Tab_Web/Tests/CombatNoDamageRecoveryChecks.py`.

- [ ] Assert that both lane controllers route `-9` positive/zero damage and `-12` kill outcomes into diagnostics with the correct lane and mob.
- [ ] Assert that a kill packet resolves a pending accepted attack and prevents a later `NO_DAMAGE` timeout for that sequence.
- [ ] Assert that an explicit zero-damage packet is recorded as a zero-damage result, not as an unobserved timeout.
- [ ] Run the focused check and confirm it fails for the specific missing behavior before changing production code.

## Task 3: Fix the confirmed root cause

Choose exactly one branch from Task 1; do not combine speculative fixes.

### Branch A — packet arrived but was not correlated

- [ ] Fix matching so it uses the available lane, sequence, mob identifier, and receive time consistently.
- [ ] Ensure `-12` kill packets resolve pending attempts before the timeout check.
- [ ] Ensure a zero-damage `-9` is distinguishable from no packet.
- [ ] Keep pending state bounded and clear it on disconnect/map/session reset.

### Branch B — accepted attack has no outcome and target is still live

- [ ] Add a single retry budget per original manual attack; never create unbounded retries.
- [ ] Retry only after the existing outcome window, only when the same target is still present/alive in the same lane, and only when the server result permits another attempt.
- [ ] Cancel retry on any `-9`, `-12`, target removal, lane disconnect, or later result for the sequence.
- [ ] If the server returns cooldown/range, wait the server's `RetryAfterMs` or reposition through the existing path; do not resend immediately.
- [ ] Record `RETRY_SCHEDULED`, `RETRY_SENT`, `RETRY_CANCELLED`, and terminal `NO_DAMAGE` with original and retry sequence IDs.

### Branch C — server sends hit/kill but ClientWeb does not apply it visibly

- [ ] Correct the specific packet parsing or mob lookup identified by the fresh trace in both controllers.
- [ ] Do not send another attack when an authoritative damage/kill packet was received.

## Task 4: Keep Game1 and Game2 behavior in parity

- [ ] Apply the selected fix symmetrically to Game1 and Game2.
- [ ] Extend the focused regression check to fail if either lane lacks the behavior.
- [ ] Verify that a late original hit cancels a scheduled retry and cannot cause a duplicate attack.
- [ ] Verify that a kill packet at 1 HP counts as a successful outcome, not `NO_DAMAGE`.

## Task 5: Verify source behavior and review the diff

- [ ] Run `python ClientWeb/PRJ_2Tab_Web/Tests/CombatNoDamageRecoveryChecks.py`.
- [ ] Run the existing focused combat source checks relevant to the changed paths.
- [ ] Inspect the final diff for matching Game1/Game2 behavior, bounded state, correct lane IDs, and no local HP fabrication.
- [ ] Report separately whether source checks passed and whether live gameplay confirmed a server damage/kill packet for every eligible attack.

## Acceptance criteria

- Every eligible attack observed in a fresh session ends in an explicit server result: positive-damage hit or kill. A rejected attack is reported with its actual reason and retry timing.
- A kill at low HP cannot be mislabeled as `NO_DAMAGE` because its packet was not correlated.
- No duplicate hit is generated by a late `-9`/`-12` arriving after a retry was scheduled.
- Game1 and Game2 have matching behavior and diagnostics.
- No client-side HP mutation is used to simulate successful damage.
- No build is started until the user explicitly asks for it.
