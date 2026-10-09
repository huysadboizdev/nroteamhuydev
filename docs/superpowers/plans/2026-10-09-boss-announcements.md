# Boss announcements and lifecycle implementation plan

Goal: show task/world/equipment bosses, suppress thief/sun/dirty bosses, announce each spawn/defeat once, and verify new turns.
Architecture: share Boss.noticePublic eligibility with ticker and board; equipment exceptions precede legacy map/disabled filters but follow training/debug exclusions. Tracker returns atomic transition results to prevent duplicate messages. Group hunting/tournament services retain one group announcement. Quest-support instances remain board-visible and ticker-quiet; normal task bosses announce. Equipment includes requested pools, hunting/tournament sealed loot and positive admin divine/seal rules. Filter excluded identities from stored board history as well.

- [x] Write/run failing actual callback tests for exclusions, equipment map/disabled exception, dedup, defeat/history/second life.
- [x] Add shared announcement policy and atomic tracker transitions; retain legacy method signatures.
- [x] Route spawn/death ticker through tracker, remove unconditional death message; make ticker queue concurrent-safe.
- [x] Expose correct Prison/Bill parent cooldown ETA; verify pair waiting and subsequent wakeup.
- [x] Compile source; run notice wire/category/persistence and lifecycle/loot/pair regressions on final candidate.
- [x] Prepare backup/maintenance deployment with restart held and detached startup; verify live notices/boss states after installation if permitted.
