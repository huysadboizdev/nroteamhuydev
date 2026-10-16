# Mob Minimum Positive Damage Implementation Plan

> **For agentic workers:** Inline execution is authorized by the user's request to fix the mob damage issue. Follow the red-green steps below.

**Goal:** Make every positive mob hit visible in both client lanes and prevent low-HP damage limits from rounding a positive hit down to zero.

**Architecture:** Handle every positive `-9` mob-hit result in Game1 and Game2, including the existing zero-damage miss path. Keep hit validation, cooldowns, XP rules, and the full-HP survival rule unchanged. Clamp the two existing server damage-limit calculations to a minimum of one only when their input hit is positive. Exercise the server path and assert both client handlers retain one-damage feedback.

**Tech Stack:** Java 17 source, existing main-based Java combat regression tests, `javac` and the workspace's existing dependency JARs.

---

### Task 1: Reproduce missing one-damage feedback

**Files:**
- Modify: `SRC/test/nro/models/services/MobPunchDeliveryTest.java`
- Create: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/MobOneDamageDisplayChecks.ps1`

- [x] Add a valid one-damage Mộc Nhân case with `maxHp < 10`; assert the mob HP drops by one and the packet reports one damage.
- [x] Add a level-mob case with `maxHp < 10`; assert the positive-hit damage override remains at least one.
- [x] Check the real Game1/Game2 packet handlers. Before the change both fail because `num204 == 1` skips hit effects and damage text.

### Task 2: Preserve the one-damage hit in both client and server

**Files:**
- Modify: `SRC/src/nro/models/mob/Mob.java`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs`
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Controller.cs`

- [x] Process positive mob-hit damage in both clients, including one; retain the zero-damage miss branch.
- [x] Change the Mộc Nhân/training-dummy cap and level-mob calculation to keep a minimum of one for positive hits.
- [x] Keep zero-damage misses at zero and preserve existing full-HP survival behavior.

### Task 3: Verify the combat regression

**Files:**
- Verify: `SRC/src/nro/models/mob/Mob.java`
- Verify: `SRC/test/nro/models/services/MobPunchDeliveryTest.java`

- [x] Run the new client and server regressions plus existing mob-punch and seven skill-cast lifecycle modes.
- [x] Compile all server production sources and run the Windows Unity build with the project's verification suite.
- [x] Preserve the previous JAR and client release, install the updated workspace JAR and requested Windows client, and verify hashes.
- [x] Keep client attack cadence unchanged; no live gameplay claim without an active controllable game session.
