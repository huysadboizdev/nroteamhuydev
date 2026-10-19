# Instant transmission: server and client freeze investigation

Scope: skill 20 (Dịch chuyển tức thời), command 123 position replies, both Game1/Game2 clients in `PRJ_2Tab_550K` and `ClientWeb/PRJ_2Tab_Web`. User reports repeated use against mobs, bosses and players can freeze the screen.

## Findings supported by source and reproduction

1. `Char.setPos()` called `Panel.hide()` for non-null panels even when hidden. `Panel.hide()` calls `mSystem.gcc()`, which invokes `Resources.UnloadUnusedAssets()` and `GC.Collect()` on the game thread. Every successful teleport position response could therefore trigger expensive cleanup during combat. The regression harness reproduced repeated hidden-panel hide calls over 1,000 position replies.
2. A command 123 reply queues `isSetPos`; the character update later assigned standing/falling status and cleared movement counters without checking whether a death packet had arrived in the meantime. The regression reproduced status 5/14 being overwritten for both ground and airborne destinations.
3. The Web client still predicted local teleport coordinates and set `telePortSkill=true` before the server replied. Movement sending refuses to run while this flag is set. Rejected/missing replies could strand this flag. All seven skill paint levels failed the local-prediction regression in both Web tabs; the main client already used authoritative local coordinates.
4. Server skill 20 had no consistent early validation for direct callers with missing/dead/disposed/cross-zone/ambiguous targets, and unconditionally applied blind state after damage. Tests reproduced stun packets/state for lethal hits and damage callbacks that moved a participant to another zone or killed the caster.

These are confirmed code/lifecycle defects. The investigation did not capture a currently hard-hung client thread and does not claim that every possible freeze has been excluded.

## Runtime observations

- Read-only thread dump of actual server PID 24236 showed no deadlock at capture.
- The running Unity Game1 client was connected on map 14, zone 0. The inspected player was dead (`statusMe=14`), with an empty receive queue, no active teleport latch and no pending effects. This observation is separate from a reproduction of a live screen freeze.
- Diagnostic data is retained under `.analysis/instant-transmission-freeze/`.

## Source corrections

- All four `Char.cs` copies now hide only displayed panels.
- A queued position is discarded after death, releasing its teleport latch without changing coordinates, death trajectory or death state.
- Both Web tabs now retain local authoritative position until command 123 arrives, matching the existing main-client behavior. Remote teleport visuals retain their existing prediction.
- `SkillService` validates one live, same-zone target before applying teleport effects, including direct boss callers. After damage it verifies the original zone and the continued validity of both participants before applying blind state, effect packets or a timer. Successful casts still complete once; existing cooldown, damage and stun duration remain unchanged.
- The command 123 payload remains exactly `int casterId, short x, short y, byte type` (9 bytes).

## Verification

Tests were written and observed failing on the original behavior before corrections.

- `Tests/InstantTransmissionFreezeChecks.ps1`: 184 assertions across main/Web Game1/Game2; repeated position replies, hidden/open/missing panels, death interleavings, both terrain states, seven teleport paint levels and remote/local behavior. Passed.
- Existing `InstantTransmissionChecks.ps1`: 86 cases per main-client tab (172 total), including delayed/absent responses and subsequent movement. Passed.
- Existing manual mob attack callbacks/retries, outgoing message queue (160,000 messages), and massacre animation gate checks. Passed.
- `InstantTransmissionLifecycleTest`: 30 isolated server groups, including 3,000 repeated valid casts across mobs/bosses/players, missing and ambiguous targets, dead/disposed/cross-zone targets, lethal damage and transferred/dead participants. Exact position payload and cooldown rejection are asserted. Passed.
- Existing `SkillCastLifecycleTest`: 11 relevant preparation, serialization, boss range, delayed cast and map/select cleanup groups. Passed.
- Changed Java source and tests compile against current application classes. Both complete runtime C# source sets compile using Unity's bundled Roslyn compiler with isolated DLL outputs: main 407 files; Web 410 files. No Unity batchmode or application packaging was used.

Reproducible verification entry points:

```powershell
python .analysis\instant-transmission-freeze\verify_server.py
python .analysis\instant-transmission-freeze\verify_clients.py
python .analysis\instant-transmission-freeze\verify_client_compile.py
```

## Build/deployment status

This task changes source and verifies isolated classes/DLLs only. No application JAR/APK/EXE or Web release was built, installed or restarted. The active `SRC/20.jar` remains the previously deployed Fide build, SHA-256 `d9347214cac0cd22365e3a9032b76f5aa685da78e2c8b0b087c4793323f0e652`.

Full gameplay acceptance on newly built server/client applications remains pending explicit build authorization and live testing. A coordinated server and client update is needed for the running applications to receive these corrections.
