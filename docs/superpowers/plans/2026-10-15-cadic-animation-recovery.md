# Cadíc animation recovery

Goal: fix map165 mobs and combat effects freezing while characters keep updating.

Approved scope: user requested implementation after the reproduced x4 mob95 frame-table failure. Work in the existing source directories, preserving pending edits. No APK/EXE/JAR release build, server restart, or JAR replacement.

Design: normalize the x4 mob95 trailing type byte from custom animation (2, missing frame table) to ordinary animation (0), matching its legacy sprite format and the client's existing mob95 animation sequences. Preserve every PNG/data byte. Guard Game1/Game2 Mob.GetFrame against missing, short, empty or invalid frame tables; keep the existing sequences and retry so late valid data can recover. Avoid changing the receive queue or swallowing exceptions across unrelated code.

- [x] Add and run regression checks against actual extracted GetFrame methods and server resource metadata. Confirm original missing-table failure.
- [x] Guard both native client GetFrame methods; verify missing/invalid tables do not abort downstream updates, valid tables apply, and late arrival recovers.
- [x] Back up mob/x4/95 with SHA-256 metadata; change only its trailing type byte to 0. Validate PNG/data integrity, sprite indexes and wire format.
- [x] Review the diff, run focused existing checks, record results and runtime limits. Leave source/data changes reviewable in place.

Validation command: `python C:/Users/Admin/Downloads/PRJ_2Tab_550K/Tests/MobFrameRecoveryChecks.py` (PowerShell Add-Type compiles only an isolated in-memory regression harness; no application packaging). Test default ordinary animation, absent/short/null/empty/negative/out-of-range custom frames, valid custom frames and retry after missing data, in both tabs. Resource check requires ordinary type 0 for mob95 x4 and unchanged sprite bounds.

Results: original checks failed in both tabs with NullReferenceException and resource typeData2; after the repair both tests pass. Also passed existing NpcImageDecodeChecks.py, android_compatibility_test.py and scoped git diff --check. Independent focused review found no correctness issues. Not verified in the full Unity update/render loop or on an Android device. APK v2 already installed is unchanged; source and local server resource are repaired, with no remote deployment claimed.
