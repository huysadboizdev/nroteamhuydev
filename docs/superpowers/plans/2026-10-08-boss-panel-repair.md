# Boss panel repair

User requested complete repair after the audit. Scope: restore server operation, fix isolated DB tests, accurately distinguish saved/pending/applied and refresh heartbeat/ACK without page reload. Keep existing queue and configuration semantics. No JAR replacement unless Java defects are reproduced.

- [x] Reproduce DB test redeclaration failure; load production helper code once without selecting live DB in isolated tests.
- [x] Add failing tests for heartbeat freshness (including missing/expired/future timestamps) and status read on disposable database.
- [x] Add authenticated GET status endpoint returning runtime, command ACKs and heartbeat age measured by database time; show queued message and refresh status every five seconds without submitting commands.
- [x] Run PHP lint, validation/isolated DB, JS contract and authenticated browser tests; verify access control and server-down status.
- [x] Start current verified JAR once through a detached launcher; verify port 14445 and fresh heartbeat. Existing commands failed identity checks; regression test reproduced session/queue mismatch, fixed consistent actor propagation and rejected anonymous saves. No automatic permanent restart policy.
- [x] Verify live save/reset on a catalog boss: +1 damage followed by immediate restore and default tombstone reset; retain audit history, prove PID unchanged and no effective stat/drop overrides left by smoke tests.
- [x] Record new evidence and limitations. Preserve existing workspace changes; no commits requested.
