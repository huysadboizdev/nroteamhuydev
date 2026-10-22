# Immediate Maintenance Disconnect Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Disconnect connected clients as soon as a durable maintenance command enters `DRAINING`, then save logout state under the exclusive gameplay barrier.

**Architecture:** Add a prepare phase to `ServerMaintenanceService` that closes session sockets before attempting `ServerMaintenanceGate.freeze`. Disconnect callbacks during paused admission are retained in a deferred set and do not run player cleanup on collector threads. Once the barrier is held, existing maintenance drains and checked logout persistence handle online, deferred, and remaining sessions idempotently.

**Tech Stack:** Java 21, existing main-style Java regression tests, current maintenance gate and `Client.LogoutActions` injection.

---

### Task 1: Test and implement two-phase coordinator ordering

**Files:**
- Modify: `SRC/src/nro/models/services/ServerMaintenanceService.java`
- Modify: `SRC/test/nro/models/services/ServerMaintenanceDrainRetryTest.java`
- Modify: `SRC/test/nro/models/services/ServerMaintenanceTest.java`

- [x] **Step 1: Add a failing prepare-before-barrier regression**

Extend `ServerMaintenanceDrainRetryTest` to construct the service with injected prepare and drain callbacks. Hold a gameplay lease, call `tick()`, and assert prepare ran exactly once while drain did not run and phase stayed `DRAINING`. Release the lease, tick again, and assert prepare remains idempotent and drain completes exactly once.

- [x] **Step 2: Run the regression and confirm the intended failure**

Run:

```powershell
$testOut = Join-Path $env:TEMP 'maint-immediate-kick-red'
& 'C:/Program Files/Java/jdk-21.0.12/bin/javac.exe' -proc:none --release 17 -encoding UTF-8 -cp 'SRC/20.jar;SRC/lib/*' -sourcepath 'SRC/src;SRC/test' -d $testOut SRC/test/nro/models/services/ServerMaintenanceTest.java SRC/test/nro/models/services/ServerMaintenanceDrainRetryTest.java
```

Expected: compile fails because the prepare callback constructor does not exist yet.

- [x] **Step 3: Add prepare callback to the service**

Add a `Prepare` functional interface and a constructor overload preserving existing test call sites. The production holder wires `ServerMaintenanceRuntime::disconnectSessions` as prepare and `ServerMaintenanceRuntime::drain` as drain. In `drainCurrent()`, invoke prepare before `freeze()` on every attempt; production behavior must therefore be idempotent.

- [x] **Step 4: Run coordinator tests**

Run `ServerMaintenanceDrainRetryTest` and `ServerMaintenanceTest`; expected: prepare occurs before every barrier attempt, saves remain after barrier only, and repeated terminal ticks do not disconnect or save again.

### Task 2: Defer logout cleanup until the gameplay barrier

**Files:**
- Modify: `SRC/src/nro/models/server/Client.java`
- Modify: `SRC/src/nro/models/server/ServerManager.java`
- Modify: `SRC/src/nro/models/services/ServerMaintenanceRuntime.java`
- Modify: `SRC/test/nro/models/server/ClientLogoutRetentionTest.java`
- Modify: `SRC/test/nro/models/services/ServerMaintenanceDrainRetryTest.java`

- [x] **Step 1: Add failing tests for deferred disconnect callbacks**

Extend `ClientLogoutRetentionTest` with an injected `LogoutActions` implementation and a disconnected session. While `ServerMaintenanceGate` is paused, call the new disconnect callback router and assert cleanup/save are not called. After calling `drainForWebMaintenance()` under an exclusive freeze, assert cleanup, player save, account save, and dispose each happen once. Call the callback again and assert no duplicate cleanup.

- [x] **Step 2: Verify the test fails for the missing callback router**

The first compile exposed the absent prepare constructor; after adding the Client regression, compilation also required the updated source files to precede the stale classes packaged in `SRC/20.jar`.

- [x] **Step 3: Add immediate socket closure and deferred logout routing**

Add a concurrent deferred-session set in `Client`. `disconnectSessionsForWebMaintenance()` snapshots online players plus active `SessionManager` sessions and invokes `session.disconnect()` only. `handleSessionDisconnect()` defers sessions while maintenance is paused, unless logout is already claimed; otherwise it calls `kickSession()`. `drainForWebMaintenance()` includes and removes deferred sessions alongside its existing player/session snapshots, then retries checked logouts and verifies none remain. Change `ServerManager.sessionDisconnect()` to use the router and retain connection-count cleanup.

- [x] **Step 4: Wire prepare phase to immediate socket closure**

Add `ServerMaintenanceRuntime.disconnectSessions()` delegating to the client transport-only disconnect method. Keep all event reconciliation, trade/map cleanup, player/account saves, and completion checks in `drain()` under the write barrier.

- [x] **Step 5: Run focused regression tests**

Run `ClientLogoutRetentionTest`, `ServerMaintenanceDrainRetryTest`, `ServerMaintenanceTest`, and `ServerMaintenanceGateTest`; expected: immediate socket closure precedes barrier acquisition, all player mutations and saves remain under the barrier, retries are idempotent, and save failures stay closed.

### Task 3: Compile and review the source change

**Files:**
- Review: all files modified in Tasks 1–2.

- [x] **Step 1: Compile touched production sources and tests**

Run with the repository JDK 21 and Lombok processor:

```powershell
$testOut = Join-Path $env:TEMP 'maint-immediate-kick-green'
New-Item -ItemType Directory -Force -Path $testOut | Out-Null
& 'C:/Program Files/Java/jdk-21.0.12/bin/javac.exe' --release 17 -encoding UTF-8 -cp 'SRC/20.jar;SRC/lib/*' -sourcepath 'SRC/src;SRC/test' -d $testOut SRC/src/nro/models/player/Player.java SRC/src/nro/models/matches/dai_hoi_vo_thuat/SuperRank.java SRC/src/nro/models/network/MySession.java SRC/src/nro/models/server/Client.java SRC/src/nro/models/server/ServerManager.java SRC/src/nro/models/services/ServerMaintenanceService.java SRC/src/nro/models/services/ServerMaintenanceRuntime.java SRC/test/nro/models/services/ServerMaintenanceTest.java SRC/test/nro/models/services/ServerMaintenanceDrainRetryTest.java SRC/test/nro/models/services/ServerMaintenanceGateTest.java SRC/test/nro/models/server/ClientLogoutRetentionTest.java
```

Expected: exit code 0. Javac reports existing annotation-processing and unchecked-operation notes.

- [x] **Step 2: Run the complete focused test set**

Run `java -cp "$testOut;SRC/20.jar;SRC/lib/*" <test-main>` for each of `nro.models.services.ServerMaintenanceDrainRetryTest`, `nro.models.services.ServerMaintenanceTest`, `nro.models.services.ServerMaintenanceGateTest`, and `nro.models.server.ClientLogoutRetentionTest`; each exits 0.

- [x] **Step 3: Inspect the final diff**

Run `git -c core.whitespace=cr-at-eol diff --check` and review the scoped Java source/test diff. No JAR was packaged or deployed.
