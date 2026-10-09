# Skill follow-up implementation plan

Goal: repair remaining server cast lifecycle defects behind high-speed skill failures.

Use the existing client real-time deadlines. On the server, preserve an early bomb release without charging cooldown, snapshot monkey level, and allow only one delayed boss/pet bomb for the original live cast, skill and zone. Preserve current damage and delay rules except target-order-dependent damage, which must use an independent value per target.

- [x] Reproduce early bomb cancellation and monkey selection/duplicate-completion errors with real service calls in an isolated Java fixture.
- [x] Fix those paths and run the Java regressions.
- [x] Guard the asynchronous bomb callback against duplicates, death, map changes, disposed state and replaced casts; use a shared daemon scheduler.
- [x] Compile all server source, run related existing regressions, and inspect the diff.
- [x] Recheck client cast ownership and rerun the Unity regression suite if client source changes.
- [x] Record evidence and limitations. Keep the installed server JAR unchanged until a tested replacement is ready and maintenance can be performed.
