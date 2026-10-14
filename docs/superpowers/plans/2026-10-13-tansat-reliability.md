# Tàn sát reliability implementation plan

**Goal:** fix automatic combat source without generating distribution artifacts.

**Architecture:** keep PickMobController as the owner of automatic combat, expose movement queue success from Service, stop the competing generic automatic attacker.

**Tech Stack:** Unity C#, PowerShell in-memory test harness, Python static verification.

- [x] Write regression harness and run against unchanged controllers; record expected failed assertions.
- [x] Add guards, valid candidate selection and single mob packet path in both controllers.
- [x] Expose TryCharMove without breaking existing charMove callers; arbitrate competing AutoAttack.
- [x] Run controller regressions and static integration checks; inspect scoped diffs and hashes of unchanged APK/JAR.
- [x] Record limitations and source changes; do not run Unity or package/build releases.
