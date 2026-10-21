# Maintenance drain retry implementation plan

**Goal:** Keep maintenance pending while admitted gameplay finishes, and expose evidence for SQL waits.

**Approved design:** User approved retrying gameplay drain timeouts on 2026-10-21. Preserve closed admission and the same durable command. Only a timeout acquiring the barrier is retryable; save/cleanup failures remain FAILED. Never save without the exclusive barrier.

**Evidence:** Supplied log command 124 timed out before drain.run. Local SQL state is OPEN revision 0, so local thread dump cannot identify the original blocker. Do not assume pool exhaustion or increase pool limits.

**Files:** ServerMaintenanceGate.java (typed timeout and bounded holder diagnostics), ServerMaintenanceService.java (retry acquisition only), LocalManager.java (pool counters and caller thread), gate/coordinator tests.

- [x] Add and run regression tests: holder timeout diagnostics; pending command retries after holder exits; no premature save or admission reopen.
- [x] Add minimal source fixes, preserving existing save-failure behavior.
- [x] Run focused gate/coordinator tests and compile modified SQL source against existing dependencies. No JAR packaging or deployment.
- [x] Review diff, document verification and remaining runtime uncertainty.

**Verification:** Gate diagnostics test initially failed because the timeout omitted the holder. Coordinator regression initially failed because a busy holder changed DRAINING to FAILED. After changes, compilation and ServerMaintenanceGateTest, ServerMaintenanceTest, ServerMaintenanceDrainRetryTest passed (JDK 21). Compilation required an elevated read of existing library files due to Windows access errors in the sandbox.

**Limits:** SQL diagnostics expose idle/total/max and caller thread; they do not prove or fix the original SQL acquisition delay. A permanently blocked gameplay operation keeps maintenance DRAINING and admissions closed, with holder diagnostics every timeout. Existing FAILED commands require another maintenance request after deployment. No source-to-JAR release or live mutation was performed.
