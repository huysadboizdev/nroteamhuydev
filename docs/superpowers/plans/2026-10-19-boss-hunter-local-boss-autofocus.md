# Boss Hunter Local Boss Autofocus Implementation Plan

> **For agentic workers:** Inline execution is authorized by the user. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make boss-hunter bots immediately select and attack a live public boss in their current zone, while retaining the existing global boss-hunting flow when no local boss is present.

**Architecture:** `BotSanBoss` will inspect the current zone's boss entities before consulting its existing registered public-boss candidates. A live local boss becomes the direct `Boss` target for the existing `PlayerService.playerMove` and `SkillService.useSkill` calls; the client cursor and HP-bar color are not inputs. If no local boss exists, the current global selection and map-travel behavior remains in effect.

**Tech Stack:** Java 17-compatible server source, JDK 21 compiler with `--release 17`, JAR archive update, PowerShell SHA-256 and process checks.

---

### Task 1: Prefer a live boss in the bot's current public zone

**Files:**
- Modify: `SRC/src/nro/models/Bot/BotSanBoss.java`

- [x] Add a helper that returns the current live `bossAttack` when it remains in the bot's zone, otherwise finds the first living `Boss` in `bot.zone.getBosses()`; reject excluded private/event maps using the existing `isMap` policy.
- [x] In `GetBoss()`, use that local boss before the global manager candidate list. Keep the current random global selection and travel behavior as the fallback.
- [x] Preserve the existing attack path through `playerMove` and `SkillService.useSkill`, so no client-side mouse targeting is added.

### Task 2: Compile, package, and install the JAR

**Files:**
- Build: `SRC/build/boss-hunter-local-boss-autofocus-<timestamp>/`
- Replace: `SRC/20.jar`
- Backup and metadata: `SRC/jar-backup/`

- [x] Compile only `BotSanBoss.java` with `--release 17` against the active JAR into an isolated staging directory. The emitted class was confirmed with `javap`; the JDK printed a ZipFileSystem `AccessDeniedException` during archive cleanup despite exit code 0.
- [x] Update a staging copy of `SRC/20.jar` with only `nro/models/Bot/BotSanBoss.class`; verify archive integrity and that all other entries are unchanged.
- [x] Confirm no Java server process and no listener on port `14445`; move the current active JAR to a unique timestamped backup before installing the candidate.
- [x] Record old/new SHA-256 hashes, timestamp, changed class, and rollback instructions in backup metadata. Confirm `SRC` contains only the active application JAR at its root.

### Verification limits

- Do not add or run tests unless the user asks for testing.
- Do not start the server; the user will run it.
- Preserve all unrelated pre-existing source and workspace changes.
