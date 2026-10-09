# Client high-speed skill review — 2026-10-09

Reviewed the working production sources in `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp` against `.analysis/client-skill-speed-20261009/source-backup`, and the implementation plan. This client directory has no Git history; findings concern the current diff, with no author/commit attribution. Read-only production review; static findings below need regression coverage. Parent agent handles Unity execution.

## Spec compliance

Monotonic timing, per-cast skill/target snapshots, duration validation, local packet matching, duplicate deadline protection, 100ms effect batches, server cooldown boundary validation, and local map/disconnect cleanup are implemented. Remote release branches do not directly send packets. Pending casts guard GameScr entrypoints.

The following cleanup and animation regressions prevent full compliance.

## Findings

1. **P1 — cancelled release animation/projectile can still send an attack.** Both `Char.stopUseChargeSkill()` methods clear flags/context but retain `skillPaint`, `skillPaintRandomPaint`, `dart`, and `arr`. For a beam whose deadline has elapsed, cancel after its projectile was created but before impact. `PlayerDart.endMe()` still calls `charBelong.setAttack()` (Game1/PlayerDart.cs:136); its attack latch remains false until the original hit and it uses current focus. Map/disconnect cleanup or the new post-release watchdog therefore does not prevent a later release packet. Cancel animation/projectiles owned by this cast and guard callbacks against a cancelled cast; do not indiscriminately clear unrelated attacks. Regression: create projectile, cancel, tick projectile/Char until completion, assert no additional packet.

2. **P2 — hit effects and existing attack animation freeze during standing/flying charge.** `updateChargeSkill()` replaces the old branches but removes both `updateEffect()` and `updateSkillPaint()` calls. Returning true also bypasses the normal calls later in `update()`. Existing `effPaints` no longer advance/injure/splash or expire, and previous `skillPaint` remains frozen for the entire charge. Restore these updates for the standing/flying branch while keeping decorative charge effect spawning throttled. Regression: seed a hit effect and existing skill animation, charge without reaching deadline, assert their indices progress.

3. **P2 — target dying just after deadline blocks new skills until watchdog.** Deadline completion sets `isCreateDark = isUseSkillAfterCharge = true`, then creates the release animation. If the target dies before the next animation tick, the dead-target branch of `updateSkillPaint()` clears animation and focus but does not cancel charge state. No projectile is made, and all guarded GameScr entrypoints remain blocked for ten seconds. Cancel immediately when that branch clears an active charge release. Regression: reach deadline with live mob, mark it dead before animation update, assert flags/movement clear immediately and no release is sent.

4. **P2 — charge cleanup removes the death movement lock.** `startDie()` sets `isLockMove = true` and status 5 before calling `stopUseChargeSkill()` for beam/monkey casts. The new ownership test lacks the old `statusMe != 14 && statusMe != 5` condition, so an active cast clears the death lock. Ownership is also set unconditionally at begin. Preserve death/freeze/hold/stone locks and capture actual cast ownership before setting the shared lock. Regression: charge then startDie, verify cast is cleared but death lock remains.

## Verification limits

Findings were traced through real Char, GameScr, Controller, and PlayerDart source. No Unity tests/build/gameplay were run by this reviewer, and no performance or gameplay acceptance is claimed. Implementation was still being finalized at review time; re-review fixes and run added cases before release.
