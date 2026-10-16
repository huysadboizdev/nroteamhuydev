# Mob Attack Hit Confirmation Implementation Plan

> **For agentic workers:** Execute each checkbox in order, keeping client and server protocol changes backward compatible.

**Goal:** Stop massacre and manual mob attacks from consuming a skill when the server rejects an out-of-range or otherwise invalid cast, while preserving PvP behavior.

**Architecture:** Extend the existing single-mob attack packet with an optional sequence/skill trailer that old servers ignore. The server selects the requested owned skill and validates/executes it atomically, then replies with the result and authoritative player/mob coordinates. Game1/Game2 clients track one pending mob cast, start cooldown only after acceptance, and reposition/retry after a range rejection.

**Tech Stack:** Unity C#, Java 17, existing client PowerShell regression harnesses, server main-based Java regression tests.

---

### Task 1: Add failing server tests

**Files:**
- Modify: `SRC/test/nro/models/services/SkillCastLifecycleTest.java`
- Test: `SRC/test/nro/models/services/SkillCastLifecycleTest.java`

- [ ] Add scenarios for atomic skill selection, server melee range rejection without cooldown, and accepted in-range casts.
- [ ] Run the existing SkillCastLifecycle test runner and confirm the new API/test fails before implementation.

### Task 2: Make server mob casts atomic and report outcomes

**Files:**
- Modify: `SRC/src/nro/models/services/SkillService.java`
- Modify: `SRC/src/nro/models/services/Service.java`
- Modify: `SRC/src/nro/models/server/Controller.java`
- Test: `SRC/test/nro/models/services/SkillCastLifecycleTest.java`

- [ ] Add an atomic mob-cast entry point that validates the skill belongs to the player, selects it under the player lock, and distinguishes accepted, out-of-range, and rejected casts.
- [ ] Reject out-of-range melee mob casts before damage/cooldown; keep player-target checks and PvP behavior unchanged.
- [ ] Parse an optional sequence/skill trailer on legacy command 54, invoke the atomic entry point when present, and reply with result plus authoritative player/mob positions.
- [ ] Keep legacy command 54 packets working when the trailer is absent.
- [ ] Run the targeted Java regression modes and compile all server production sources.

### Task 3: Add client result-tracking regression tests

**Files:**
- Create: `Assets/Scripts/Assembly-CSharp/MobAttackResultTracker.cs`
- Create: `Tests/MobAttackResultChecks.ps1`
- Test: `Tests/MobAttackResultChecks.ps1`

- [ ] Write tests proving pending attacks block duplicate casts, accepted results consume cooldown once, range/general rejection do not consume cooldown, and clearing the tracker releases pending state.
- [ ] Run the test and confirm it fails before implementation.

### Task 4: Integrate result tracking into Game1 and Game2 clients

**Files:**
- Modify: `Assets/Scripts/Assembly-CSharp/Game1/Service.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game2/Service.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game1/Controller.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game2/Controller.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game1/Mod/XMAP/PickMobController.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game2/Mod/XMAP/PickMobController.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game1/ModFunc.cs`
- Modify: `Assets/Scripts/Assembly-CSharp/Game2/ModFunc.cs`

- [ ] Append sequence and skill ID only to single-mob command 54 packets; keep other packet shapes unchanged.
- [ ] Handle the server result on the main-thread message path, synchronize coordinates after rejection, and clear pending state on disconnect/map transition.
- [ ] Make massacre pause while its cast is pending, reposition and retry on range rejection, and record cooldown/advance skill only after acceptance.
- [ ] Make manual mob auto-attacks use the same acknowledgement path; leave player attacks unchanged.
- [ ] Run result-tracker, massacre range/animation, and outgoing queue regressions.

### Task 5: Verify protocol and source changes

**Files:**
- Verify: server and client changed files only.

- [ ] Check legacy command 54 requests without trailers retain their old handling.
- [ ] Check the server result command is unused by other code and ignored safely by older clients.
- [ ] Run all relevant existing server/client tests and `git diff --check` for touched files.
- [ ] Do not build or replace JAR/EXE/APK unless explicitly requested.
