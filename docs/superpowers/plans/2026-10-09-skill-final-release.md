# Skill final release plan

User authorized fixing the remaining intrinsic cooldown defect and installing the verified skill release.

1. Reproduce overlapping intrinsic cooldown timers, stale casts and replaced/disposed skills through the real EffectSkill update/service paths.
2. Keep one pending intrinsic timer per skill, capture skill identity and cast timestamp, and release only eligible timers. Preserve existing mana refill and packet format.
3. Compile all source and run lifecycle, intrinsic, HP and boss damage regressions directly against a new candidate JAR. Compare source hashes before installing.
4. Invoke the existing Maintenance.startSeconds through Java Attach, wait for data-saving shutdown, and verify no server process/listener remains.
5. Move the installed JAR to a unique timestamped backup, verify hashes, install candidate, archive excess application JARs without touching libraries/tools, and record metadata.
6. Start the updated server hidden, verify port ownership and startup logs, and update the release report. Client release remains the already verified build.
