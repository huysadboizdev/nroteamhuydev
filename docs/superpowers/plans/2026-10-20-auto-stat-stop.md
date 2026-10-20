# Auto stat completion implementation plan

**Goal:** Finish auto stat allocation near its requested target or when the server rejects an upgrade at the current cap, with one notice and no repeated requests.

**Approved design:** The user approved implementation on 2026-10-20. HP/KI advance in units of 20; damage, defense and critical in units of 1. Stop when less than one unit remains. Server limit notices finish the active run. A missing response stops with a separate timeout notice, never a false success.

**Architecture:** Keep allocation in each tab's ModFunc, using one step method shared by the browser coroutine and desktop thread. Track one pending request, a 5-second deadline and a run identifier so a replaced worker cannot send again. Route known server notices through an idempotent stop method in Controller.

**Files:** Game1 and Game2 ModFunc.cs/Controller.cs in ClientWeb/PRJ_2Tab_Web and C:/Users/Admin/Downloads/PRJ_2Tab_550K; Tests/AutoPointChecks.py in the native client.

**Validation:** Extract actual C# allocation methods into an in-memory PowerShell Add-Type harness. Exercise close targets, exact targets, both stat units, single-step defense/critical, delayed responses, missing pet, no potential, server limits, timeout, replaced workers and inactive notices in both tabs and both client trees. Assert both browser and desktop drivers call the step method. Do not package APK, EXE or JAR.

- [x] Write and run the regression harness; confirmed failure before implementation (bounded allocation worker missing).
- [x] Implement bounded allocation and pending-request handling in both tabs, both clients.
- [x] Route server cap and insufficient-potential notices, preserving unrelated notices.
- [x] Run regression harness and inspect scoped diffs; leave unrelated working changes intact.

Verification: `python C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/AutoPointChecks.py` passed all six combinations: native Game1/Game2 and Web Game1/Game2 compiled in desktop and browser modes. The harness invokes actual extracted allocation methods and Controller notice dispatch statements in memory. It checks the browser coroutine's send/yield/finish behavior. No game build, packaging or live-server modification was performed. Live gameplay remains untested.
