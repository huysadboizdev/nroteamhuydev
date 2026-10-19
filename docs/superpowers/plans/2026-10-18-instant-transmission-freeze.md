# Instant Transmission Freeze Implementation Plan

> **For agentic workers:** Execute inline with the executing-plans workflow; the user has authorized investigation and correction on both server and client. Do not build application JAR/APK/EXE or deploy in this task.

**Goal:** Remove repeated forced resource cleanup from instant-transmission combat and prevent stale target/terminal-state packets.

**Architecture:** Keep command 123 and skill 20 unchanged on the wire. On the client, close only panels that are actually displayed and apply teleport position without erasing death state. On the server, validate the instant-transmission target before side effects, and emit stun only if caster and target remain alive together in the original zone after damage.

**Tech Stack:** Java 17 source/classes; C# game clients Game1/Game2 in PRJ_2Tab_550K and ClientWeb/PRJ_2Tab_Web; PowerShell/C# source-extraction regression harnesses.

## Task 1: Reproduce client side effects

- [x] Add behavioral regression cases to `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/InstantTransmissionFreezeChecks.ps1` using the actual `Char.setPos` method and actual `if (isSetPos)` update block.
- [x] Verify that 1,000 position replies with both panels closed produce zero `Panel.hide()` calls, while an open panel closes once.
- [x] Verify that a pending position followed by death (status 5 or 14) does not reset the terminal state to standing/falling.
- [x] Exercise the main and Web Game1/Game2 copies and retain before/after outputs in `.analysis/instant-transmission-freeze`.

## Task 2: Correct client position handling

Files: `Assets/Scripts/Assembly-CSharp/Game1/Char.cs` and `Game2/Char.cs` under both client roots.

- [x] Change panel guards inside `setPos` to `GameCanvas.panel != null && GameCanvas.panel.isShow` and the matching `panel2` guard. No forced garbage collection is necessary for a panel already closed.
- [x] Preserve status 5/14 when applying a queued coordinate reply: only assign terrain standing/falling state inside `if (statusMe != 14 && statusMe != 5)`.
- [x] Preserve current movement synchronization and the release of `telePortSkill` for accepted replies.

## Task 3: Reproduce server lifecycle defects

Files: `SRC/test/nro/models/services/InstantTransmissionLifecycleTest.java`, `SRC/src/nro/models/services/SkillService.java`.

- [x] Capture actual position, attack and effect packets through a test Service instance with isolated fixtures; do not contact production SQL.
- [x] Reject source/target in different zones, dead targets, missing targets, disposed targets and ambiguous player-plus-mob requests before location/HP/mana/cooldown changes.
- [x] Verify a lethal hit does not emit a new blind effect or timer for the dead target.
- [x] Verify a target/caster transferred by damage callbacks receives no old-zone blind effect.
- [x] Stress repeated valid casts for mobs and characters; assert one position and one completion per accepted cast and the existing cooldown gate still rejects immediate repeats.

## Task 4: Correct server lifecycle

- [x] Add a narrow instant-transmission validation helper in `SkillService` for source/target zone, location, point and alive state; call it before general cast processing and again for direct `useSkillAttack` callers.
- [x] Save the cast's zone and recheck both participants after damage before applying blind state and effect/timer packets. Keep successful-cast cooldown/mana completion exactly once.
- [x] Leave damage coefficients, duration and wire layout unchanged.

## Task 5: Verification and handoff

- [x] Run client regression harnesses for repeated position replies, death interleaving, delayed/absent replies, manual mob attacks and outgoing queue behavior.
- [x] Compile changed Java sources and tests only; run the new lifecycle suite and affected existing skill lifecycle cases.
- [x] Verify diffs preserve changes already present in both working trees and inspect client pair symmetry.
- [x] Document runtime evidence separately from standalone tests. The initial server thread dump had no deadlock; the initial client snapshot was connected with an empty receive queue and a dead player, rather than a currently captured hard hang.
- [x] Report application build/deployment as pending explicit user instruction.
