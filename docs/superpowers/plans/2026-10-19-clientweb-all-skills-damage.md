# ClientWeb All Mob Skills Damage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Make every legal, mob-targeting attack skill deal server-authoritative damage or produce its correct delayed/kill outcome in both manual attacks and Tàn Sát, across Game1 and Game2.

**Architecture:** Keep mob HP and damage authoritative on the server. Inventory each skill by template type and execution path, then route every offensive mob skill through one confirmed cast lifecycle that reports rejection, accepted cast, applied hit, or kill distinctly. Manual attack and Tàn Sát share target/range/cooldown rules; neither fabricates HP or retries an accepted delayed cast.

**Tech Stack:** Unity C# ClientWeb, Java server (`Controller`, `Service`, `SkillService`), existing combat diagnostics and result tracker, Python source regression checks, focused Java delivery tests. No APK, EXE, JAR, or WebGL build unless explicitly requested.

## Implementation status — 2026-10-19

- Added the shared ClientWeb offensive skill catalog and routed both TÃ n SÃ¡t lanes through it. Tự Sát keeps its self-centered `-45` behavior and now carries an optional confirmed-cast envelope when used against mobs.
- Added server `PREPARING` result code for charge/self-destruct casts, retry timing from the server preparation state, and truthful rejection when a type-4 special executor does not start.
- Updated combat traces with per-skill damage windows and unattributed area-hit reporting; legacy damage packet formats remain unchanged.
- Focused Python/C# harnesses and Java lifecycle/punch regressions pass. A fresh WebGL build is now installed at `ClientWeb/PRJ_2Tab_Web/Builds/WebGL`; static serving passed. Live combat logs remain pending a gameplay run against the server.

## Verified execution matrix

| Skill IDs | Template type | Server execution path | Result handling |
| --- | --- | --- | --- |
| 0, 2, 4, 17 | 1 | Direct melee/combo via `useSkillAttack` | Confirmed single-mob hit, range and cooldown checks |
| 1, 3, 5, 9, 20 | 1 | Projectile or movement attack via `useSkillAttack` | Confirmed single-mob hit; canonical skill effects retained |
| 10 | 1 | QCKK charge, then primary and nearby mob damage | `PREPARING` until the 4-second server window ends; area outcomes marked unattributed |
| 11 | 1 | Makankosappo charge, then canonical laser attack | `PREPARING` until the 3-second server window ends |
| 14 | 3 | Self-centered Tự Sát preparation and delayed/area executor | Optional `-45` confirmation envelope; legacy payload is restored when its marker is absent |
| 24, 25 | 4 | Special-skill start and normal special update | Accepted only when the special executor confirms that it started; area outcomes marked unattributed |
| 26 | 4 | Ma Phong Ba control and containment effect | Excluded from mob-damage rotation because its canonical server path does not reduce mob HP |

The client catalog and server allowlist contain the same offensive IDs: `0, 1, 2, 3, 4, 5, 9, 10, 11, 14, 17, 20, 24, 25`. Buff/self-only IDs and control-only Ma Phong Ba (`26`) are excluded from mob damage. Java delivery coverage exercises a direct projectile and charged area damage, while punch coverage exercises melee delivery. Live browser behavior remains unverified until a fresh build is explicitly requested and deployed.

---

## Findings and boundaries

- ClientWeb's manual mob path is in `Game1/Char.cs` and `Game2/Char.cs`; Tàn Sát chooses and dispatches skills in each lane's `Mod/XMAP/PickMobController.cs`.
- ClientWeb marks confirmed single-mob command `54` casts. For T? S?t, it appends optional confirmation metadata after the legacy `-45` status byte; the server parses the complete marker/sequence/skill tuple and rewinds the reader when the marker is absent, preserving the legacy payload.
- `SkillService.useSkillForMob` accepts validated offensive template types 1, 3, and 4. Type 1 uses the canonical targeted executor, type 3 is allowed only for T? S?t (14), and type 4 uses the special-skill executor for damaging skills 24 and 25. Ma Phong Ba (26) remains a control skill and is excluded from damage selection.
- Damage and kill are separate server packets (`-9` and `-12`); attack result `-48` only confirms whether the cast was accepted, rejected, out of range, or cooling down. A delayed cast must not be treated as a missed hit before its server-side execution window ends.
- Preserve range, cooldown, mana, stamina, target state, map/zone, immunity, and PvP rules. “Every skill” means every offensive skill that is valid for the selected mob under server rules. Buffs and self-only skills remain non-damage actions unless the skill definition explicitly damages mobs through its own server execution path.
- Do not add a client-side HP decrement, retry an accepted cast blindly, or change the `-9`/`-12` wire format without an explicit compatibility design for legacy clients.
- Existing combat logs show `HIT_MATCH`, `KILL_MATCH`, `NO_DAMAGE`, and `NO_ACK` events, but the saved browser sessions predate the current source fix and cannot establish behavior of a fresh build. Use fresh logs after an explicitly requested build for live confirmation.

Inventory correction: the confirmed mob executor accepts offensive template types 1, 3, and 4. Type 3 is eligible only for Tự Sát (14). Type-4 skills 24 and 25 damage mobs through the special update path; Ma Phong Ba (26) is a control/containment skill and its normal server executor does not reduce mob HP, so it is deliberately excluded from the damage rotation.

## Files in scope

- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Char.cs` and `Game2/Char.cs` — manual skill dispatch, pending manual retry, and selected target state.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/PickMobController.cs` and `Game2/Mod/XMAP/PickMobController.cs` — Tàn Sát skill enumeration, target selection, approach movement, and result handling.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Service.cs` and `Game2/Service.cs` — confirmed command `54`, attack origin/skill metadata, lane isolation, and send cancellation.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Service.cs` and `Game2/Service.cs` — preserve the command `-45` payload for type-3 self-use or type-4 special casts; add confirmation metadata only for skill categories the inventory proves can damage mobs.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/MobAttackResultTracker.cs` and `ClientWebCombatDiagnostics.cs` — bounded cast lifecycle and outcome classification.
- `ClientWeb/PRJ_2Tab_Web/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs` and `Game2/Controller.cs` — `-48`, `-9`, and `-12` result parsing and mob state application.
- `SRC/src/nro/models/server/Controller.java`, `SRC/src/nro/models/services/Service.java`, and `SRC/src/nro/models/services/SkillService.java` — confirmed cast request, server validation/execution, and result response.
- `ClientWeb/PRJ_2Tab_Web/Tests/AllSkillsMobDamageChecks.py` — cross-lane source contract and skill-route regression checks.
- `SRC/test/nro/models/services/MobSkillDeliveryTest.java` and `SRC/test/nro/models/services/MobPunchDeliveryTest.java` — server damage, packet, cooldown, and rejection coverage.

## Task 1: Establish a skill and execution-path matrix

**Files:** Read the skill definitions and the client/server paths listed above; create `ClientWeb/PRJ_2Tab_Web/Tests/AllSkillsMobDamageChecks.py` and `SRC/test/nro/models/services/MobSkillDeliveryTest.java`.

- [ ] Inventory every skill template available to the player by ID, display name, template type, `isBuffToPlayer`, `isUseAlone`, target mode, cooldown, range, and server executor.
- [ ] Mark each skill as one of: direct mob hit, projectile/special hit, area/self-cast that can damage mobs, buff, self-only with no mob damage, or unsupported/rejected. Record which ones have delayed server damage.
- [ ] Trace the four user-visible cases independently: manual punch, manual blast/projectile, Tàn Sát punch, and Tàn Sát blast/projectile. Include all configured attack skills, not just the currently selected skill.
- [ ] Add failing source checks proving that Game1 and Game2 use the same confirmed-cast protocol and result handling for each eligible mob skill category.
- [ ] Add failing server tests for at least one representative from each damage category discovered in the inventory; assert HP decreases and `-9` or `-12` is emitted exactly once after accepted execution.
- [ ] Assert that range, cooldown, no mana/stamina, dead/removed target, and invalid skill requests do not reduce HP or emit a positive-damage result.
- [ ] Run `python ClientWeb/PRJ_2Tab_Web/Tests/AllSkillsMobDamageChecks.py` and record its expected failures before changing production code.
- [ ] Compile the Java regression into a temporary directory without packaging a JAR:

```powershell
$testOut = Join-Path $env:TEMP 'clientweb-mob-skills-tests'
New-Item -ItemType Directory -Force $testOut | Out-Null
& 'C:\Program Files\Java\jdk-21.0.12\bin\javac.exe' --release 17 -encoding UTF-8 `
  -cp 'SRC/20.jar;SRC/lib/*' -sourcepath 'SRC/src;SRC/test' -d $testOut `
  'SRC/test/nro/models/services/PinkFixture.java' `
  'SRC/test/nro/models/services/MobSkillDeliveryTest.java'
if ($LASTEXITCODE -ne 0) { throw 'MobSkillDeliveryTest compilation failed' }
& 'C:\Program Files\Java\jdk-21.0.12\bin\java.exe' `
  -cp "$testOut;SRC/20.jar;SRC/lib/*" nro.models.services.MobSkillDeliveryTest
```

Expected before implementation: the new assertions fail for at least one currently unsupported eligible skill category; the fixture prints `PASS MobSkillDeliveryTest` only after all requested delivery and rejection cases pass.

## Task 2: Make the server confirmed-cast executor cover every offensive mob skill

**Files:** Modify `SRC/src/nro/models/services/SkillService.java`; add/modify `SRC/test/nro/models/services/MobSkillDeliveryTest.java` and `MobPunchDeliveryTest.java`.

- [ ] Resolve the requested skill ID while holding the existing player lock, then validate ownership, target zone/liveness, status effects, mana, stamina, range, and cooldown before executing it.
- [ ] Route each matrix category to the existing canonical server executor: direct skills through `useSkillAttack`, projectile/special skills through their supported special-skill executor, and mob-damaging self-casts through the existing delayed/area executor. Do not route a self-cast through `useSkillForMob` if that changes its target semantics.
- [ ] Keep type-2 buffs and non-damaging self-only skills out of the mob-damage success path. For a valid self-cast area attack, require the server executor to confirm that the cast started and let its normal server update apply mob damage later.
- [ ] If the matrix confirms a mob-damaging `-45` cast is currently unacknowledged, add an optional trailing confirmation envelope after that status's existing payload. Parse it only when the marker, sequence, and requested skill ID are complete; leave legacy messages without the marker on the current path. Return `-48` only after the selected skill has actually been admitted, using the no-single-mob sentinel already supported by `Service.sendMobAttackResult`.
- [ ] Return `ACCEPTED` only after the server has admitted the correct skill execution. Return the existing rejection/range/cooldown result for invalid or premature attempts; never send an accepted result for a branch that did nothing.
- [ ] Preserve the existing range and melee cooldown grace rules. Keep skill-specific range checks and execution timing tied to the template or canonical executor rather than duplicating approximate rules in ClientWeb.
- [ ] Extend the server tests for each attack category and for two rapid requests, verifying that accepted attacks apply exactly one intended damage execution and the immediate duplicate is rejected without a second hit.
- [ ] Compile `PinkFixture.java`, `MobSkillDeliveryTest.java`, `MobPunchDeliveryTest.java`, and `SkillCastLifecycleTest.java` with the same `javac --release 17 -encoding UTF-8 -cp 'SRC/20.jar;SRC/lib/*' -sourcepath 'SRC/src;SRC/test' -d $testOut` pattern from Task 1.
- [ ] Run `java -cp "$testOut;SRC/20.jar;SRC/lib/*" nro.models.services.MobSkillDeliveryTest` and `java -cp "$testOut;SRC/20.jar;SRC/lib/*" nro.models.services.MobPunchDeliveryTest`; each must print its `PASS` line.
- [ ] Run `java -cp "$testOut;SRC/20.jar;SRC/lib/*" nro.models.services.SkillCastLifecycleTest <mode>` for each existing mode: `early-bomb`, `early-ball`, `early-laser`, `bomb-map-interleave`, `bomb-target-order`, `delayed-valid`, `delayed-map`, `delayed-death`, `delayed-skill`, `delayed-cancel`, `delayed-dispose`, `delayed-recast`, `map-clear`, `select-cancel`, `serialized-use`, `boss-cast-valid`, `boss-cast-range`, `mob-cast-valid`, `mob-cast-range`, `mob-retry-expired`, `mob-retry-active`, `monkey-level`, `monkey-restart`, and `monkey-repeat`; each invocation must print `PASS <mode>`.

## Task 3: Unify manual attack dispatch and outcomes in both ClientWeb lanes

**Files:** Modify `Game1/Char.cs`, `Game2/Char.cs`, `Game1/Service.cs`, `Game2/Service.cs`, `Game1/Controller.cs`, `Game2/Controller.cs`, `MobAttackResultTracker.cs`, and `ClientWebCombatDiagnostics.cs`; modify `SRC/src/nro/models/server/Controller.java` and `SRC/src/nro/models/services/Service.java` only if Task 1 confirms a damaging `-45` category needs confirmation metadata.

- [ ] Carry lane, attack origin (`MANUAL` or `TAN_SAT`), attempt sequence, skill ID/type, and target mob ID through the confirmed command `54` path for single-mob skills. For any mob-damaging self-cast confirmed in Task 1, carry lane, origin, sequence, and skill ID through its optional `-45` confirmation envelope while preserving its legacy payload.
- [ ] Register a pending result for every offensive mob skill supported by the matrix, including skills that use a delayed server executor; do not silently omit tracking because a skill uses a different animation/type.
- [ ] Keep Game1 and Game2 state isolated by lane. Cancel pending casts on send failure, disconnect, target removal, map change, or session reset.
- [ ] Distinguish `CAST_ACCEPTED`, `HIT`, `KILL`, `ZERO_DAMAGE`, `REJECTED`, `OUT_OF_RANGE`, `COOLDOWN`, and true `NO_ACK`/`NO_DAMAGE_TIMEOUT` in diagnostics. The damage timeout for a delayed skill must be based on its real server execution window, not a universal two-second assumption.
- [ ] Match single-target `-9`/`-12` outcomes to pending casts using lane, target, and packet order/time available in the existing protocol. For area casts, record cast acceptance and the set/count of damage packets in the execution window; because legacy `-9`/`-12` packets do not carry attacker sequence IDs, mark overlapping or unattributable hits ambiguous instead of claiming a precise skill match.
- [ ] Retain the fixed `-12` behavior: a kill packet for a live `status == 0` mob must call `startDie()` and record the kill; only null or already-dying targets may be ignored.
- [ ] Update regression checks to fail if either lane loses any outcome or category handling. Run the checks and the existing `CombatAttackParityChecks.py`, `CombatDeathTraceChecks.py`, and `CombatNoDamageRecoveryChecks.py`.

## Task 4: Route Tàn Sát through the same legal skill lifecycle

**Files:** Modify `Game1/Mod/XMAP/PickMobController.cs`, `Game2/Mod/XMAP/PickMobController.cs`, and `Game1/ModFunc.cs`/`Game2/ModFunc.cs` only where their current skill cooldown/result callback needs to use the shared cast lifecycle.

- [ ] Replace the current broad `type >= 1 && type <= 4` selection plus hard-coded ID exclusions with the eligibility categories from Task 1. Do not choose a buff as a mob attack; include every actual mob-damaging skill even if its current numeric ID is in an exclusion list.
- [ ] Preserve the existing Tàn Sát filters for selected mobs and players, and preserve minimum target liveness and map membership checks before a cast is sent.
- [ ] For a skill outside its legal range, approach to the range computed by the server-compatible skill range and wait for the target position confirmation before resending. Do not charge skill cooldown or advance the attack-skill cursor when no attack packet was sent.
- [ ] For server cooldown, wait the returned `RetryAfterMs`; for out-of-range, reposition and try once the refreshed target state is valid. Do not retry rejected resource/status cases in a tight loop.
- [ ] For an accepted projectile or delayed area cast, wait for its category-specific resolution window; cancel pending repeats on `-9`, `-12`, target removal, target switch, lane disconnect, or later result.
- [ ] Add parity checks proving both lanes enumerate all eligible matrix rows, exclude non-damaging buffs, and produce matching dispatch/result behavior.
- [ ] Run `MassacreSkillChecks.py`, `AutoAttackSkillChecks.py`, `CombatAttackParityChecks.py`, and `AllSkillsMobDamageChecks.py`.

## Task 5: Verify packet compatibility and source parity

**Files:** Review `SRC/src/nro/models/server/Controller.java`, `SRC/src/nro/models/services/Service.java`, both ClientWeb lane controllers/services, and the full diff.

- [ ] Verify command `54` still uses the legacy direction byte and optional confirmed marker `0xA7`, sequence, and skill ID in the exact order the server parser expects. If Task 1 adds confirmed self-casts, verify the optional `-45` marker follows the complete legacy status payload and that messages without it retain legacy behavior.
- [ ] Verify `-48` result parsing agrees with the server's result field order and numeric status codes.
- [ ] Verify `-9` and `-12` payloads remain unchanged so installed legacy clients continue parsing them; add a protocol version/capability design before any payload expansion.
- [ ] Review all accepted skill categories against the matrix and confirm each has one canonical server execution path, one valid client dispatch path, and a clear terminal outcome.
- [ ] Compare Game1 and Game2 modified sections for identical behavior apart from lane ID and class names. Review `git diff --check` and the final diff for accidental local HP writes or unbounded retries.

## Task 6: Regression and live acceptance

**Files:** Test only the source and existing browser logs; no artifact packaging in this task.

- [ ] Run `python ClientWeb/PRJ_2Tab_Web/Tests/AllSkillsMobDamageChecks.py`.
- [ ] Run `python ClientWeb/PRJ_2Tab_Web/Tests/CombatAttackParityChecks.py`, `CombatDeathTraceChecks.py`, `CombatNoDamageRecoveryChecks.py`, `MassacreSkillChecks.py`, and `AutoAttackSkillChecks.py`.
- [ ] Run the focused Java tests `MobSkillDeliveryTest`, `MobPunchDeliveryTest`, and `SkillCastLifecycleTest` against source classes without packaging `SRC/20.jar`.
- [ ] After a WebGL build has been explicitly requested and deployed, capture fresh log sequences for manual punch, manual blast/projectile, Tàn Sát punch, Tàn Sát blast/projectile, a low-HP kill, out-of-range, cooldown, no-resource, and a delayed skill.
- [ ] For each legal accepted attack, require a server damage packet or kill packet within its category's expected execution window. For each rejection, require the real server reason and no client-fabricated damage.
- [ ] Compare Game1 and Game2 traces and confirm there are no duplicate retries, missed kills, false `NO_DAMAGE` reports for delayed hits, or pending-state leaks after disconnect/target change.
- [ ] Report source test results and live gameplay results separately. Do not claim live resolution from source tests alone.

## Acceptance criteria

- Every eligible offensive skill in the inventory is selectable and dispatchable through manual mob attack and Tàn Sát in both Game1 and Game2.
- A server-accepted direct skill applies mob HP damage or a kill through the normal server combat path; a delayed/projectile/area cast reaches its normal later damage path and is not retried as a miss while pending.
- Server rejection, range, cooldown, invalid target, insufficient resource, or immunity remains authoritative and is reported accurately.
- Tàn Sát rotates across eligible offensive skills without numeric-ID-only omissions; buffs and non-damaging self-use skills are not mislabeled as mob attacks.
- No client-side HP fabrication, duplicate hits, unbounded retry, `-9`/`-12` legacy protocol break, or Game1/Game2 lane-state leakage is introduced.
- No JAR/APK/EXE/WebGL artifact is built until the user explicitly asks for a build.
