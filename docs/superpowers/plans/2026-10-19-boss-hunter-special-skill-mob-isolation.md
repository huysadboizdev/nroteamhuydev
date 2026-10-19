# Boss Hunter Special Skill Mob Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep boss-hunter special attacks focused on bosses, prevent currency-packet NPEs for sessionless actors, and update the active server JAR.

**Architecture:** Expose the existing bot type through a focused `Bot.isBossHunter()` predicate. Use it in `SkillService.updateSkillSpecial` to omit the secondary mob attack loop for boss hunters while preserving boss and player target handling. Make `Service.sendMoney` return early when there is no player session.

**Tech Stack:** Java 17, `javac`, JAR ZIP packaging, PowerShell SHA-256/process checks.

---

### Task 1: Scope special-skill mob hits to non-hunter actors

**Files:**
- Modify: `SRC/src/nro/models/Bot/Bot.java`
- Modify: `SRC/src/nro/models/services/SkillService.java`

- [x] **Step 1: Expose the boss-hunter identity**

Add this method to `Bot`:

```java
public boolean isBossHunter() {
    return this.type == 2;
}
```

- [x] **Step 2: Skip secondary mob hits for boss hunters**

Import `nro.models.Bot.Bot` in `SkillService` if needed. In `updateSkillSpecial`, change only the mob-target loop guard from:

```java
if (!player.isBoss) {
```

to:

```java
if (!player.isBoss && (!(player instanceof Bot bot) || !bot.isBossHunter())) {
```

Keep the player-target loop and its boss target selection unchanged.

### Task 2: Make money updates safe for actors without sessions

**Files:**
- Modify: `SRC/src/nro/models/services/Service.java:1395`

- [x] **Step 1: Guard before creating a client message**

At the start of `sendMoney`, return when `pl` or `pl.getSession()` is null. Read the session once and use that captured reference for the protocol version check:

```java
public void sendMoney(Player pl) {
    if (pl == null) {
        return;
    }
    MySession session = pl.getSession();
    if (session == null) return;
    Message msg;
    try {
        msg = new Message(6);
        if (session.version >= 214) {
```

Prefer assigning `pl.getSession()` to a local `MySession session` and checking `session.version` so a disconnect between the check and the read cannot cause this NPE.

### Task 3: Compile, package, and replace the active JAR

**Files:**
- Build: `SRC/build/boss-hunter-special-skill-mob-isolation-<timestamp>/`
- Replace: `SRC/20.jar`
- Backup and metadata: `SRC/jar-backup/`

- [x] **Step 1: Compile the changed classes**

Compile `Bot.java`, `SkillService.java`, and `Service.java` with Java 17 against `SRC/20.jar;SRC/lib/*`. The working copies contain unrelated uncommitted changes. Preserve them, and inspect the active class entries for evidence that those existing changes are already present before packaging current source output. Do not include an unrelated source change that is absent from the active JAR. The compiler may emit nested helper classes; replace only the three top-level classes named below because no nested helper class source is changed.

- [x] **Step 2: Build a candidate from the active JAR**

Replace only `nro/models/Bot/Bot.class`, `nro/models/services/SkillService.class`, and `nro/models/services/Service.class` in a copy of `SRC/20.jar`. Keep all other archive entries byte-for-byte unchanged. Validate ZIP CRC and confirm the three entries match compiler output.

- [x] **Step 3: Verify the server is stopped and archive the current JAR**

Confirm no server Java process and no listener on port `14445`. Move the active `SRC/20.jar` to a unique timestamped path in `SRC/jar-backup` before installation. Do not start the server.

- [x] **Step 4: Install and record recovery metadata**

Install the candidate as `SRC/20.jar`. Verify the backup SHA-256 equals the pre-install active JAR hash and the new active SHA-256 equals the candidate hash. Write metadata with hashes, timestamp, compiled class names, and restore instructions. Confirm `SRC` contains only the active application JAR at its root.

### Verification and limits

- Do not add or run tests unless the user requests testing.
- Verify compilation, JAR CRC, exact replaced class entries, backup/new SHA-256, and server-stopped status.
- Do not start the server; the user will run it.
- Preserve all unrelated pre-existing source changes. If the current `Service.java` or `SkillService.java` contains pending source changes not represented by the active JAR, inspect before packaging and do not silently include unrelated behavior in the JAR.
