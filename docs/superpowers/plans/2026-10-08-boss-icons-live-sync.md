# Boss resource icons and immediate live sync

User approved immediate application to living bosses and web reflection of game state.

- [x] Resolve head_avatar then actual head part frames; copy existing PNG x4/x2/x1 to boss assets. Audit 144 entries, correct HuntingTrio web mappings and mark player-dependent portraits. Preserve game outfits.
- [x] Test image coverage and HTTP loading including Baby, Black Goku, thief and sun.
- [x] Include configs in status API; refresh configs/ACK/runtime while preserving drafts. Display fresh actual per-phase HP/Dame with defaults and pending changes labeled separately.
- [x] Default edits/reset to alive mode; keep next-spawn option. Test immediate reset and blank-default semantics.
- [x] Reproduce missing HP broadcast with Java wire test; send existing -30/subcommand 9 after changed alive HP. No packets for unchanged HP, next mode, dead or detached bosses.
- [x] Fresh Java/PHP/JS/browser tests and candidate JAR checks. Saved maintenance, SQL snapshot, timestamped JAR backup and SHA-256 before install; only active application JAR in SRC.
- [x] Start one detached JVM; verify web live edits/reset, HP ratio, damage, ACK/runtime and unchanged PID. Restore smoke defaults, preserve audits and document client test limits.
