# Boss Hunter Special Skill Mob Isolation Design

## Problem

The supplied stack trace originates from a boss hunter using a special skill. `SkillService.updateSkillSpecial` applies the special skill to nearby mobs when `player.isBoss` is false. A bot hunter is marked `isBot`, not `isBoss`, so it can kill ordinary mobs while attacking a boss. `Mob.hutItem` then auto-picks up the mob drops for bots because `NewBot` gives them a long-running pickup charm. Currency pickup calls `Service.sendMoney`, which reads `getSession().version`; bots have no session, causing the repeated `NullPointerException`.

## Goals

- Keep boss hunters' special skills effective against their selected boss.
- Prevent boss hunters' special skills from attacking ordinary mobs in the same area.
- Make `Service.sendMoney` safe for any player-like actor without a session.
- Preserve special-skill behavior for regular players and other bot types.
- Avoid starting the server during implementation or verification; the user runs it.

## Non-goals

- Disable or rewrite mob drops for players.
- Remove bot pickup charms or change bot inventory behavior globally.
- Change which bosses are eligible or how boss targets are selected.
- Change boss hunter attack, damage, or skill ownership settings.

## Design

Add an explicit boss-hunter predicate to `Bot`, backed by its existing bot type (`type == 2`). In `SkillService.updateSkillSpecial`, continue the existing mob-area attack loop for normal players and non-hunter actors, but skip that loop for a boss-hunter bot. The direct boss target and existing player-target portion of the special-skill update remain unchanged.

In `Service.sendMoney`, return before constructing a message if the player or its session is null. Currency changes already happen in `InventoryService.addItemBag`; the guard only suppresses a client packet when there is no connected client to receive it. This prevents the same null-session failure if a sessionless actor reaches this method through another path.

## Alternatives considered

1. **Boss hunters do not hit ordinary mobs; add a session guard. Recommended.** Prevents the collateral mob kill and its unnecessary loot flow, while retaining a general guard for sessionless callers.
2. **Keep area damage and disable bot auto-pickup.** Avoids this pickup path but still spends boss-hunting attacks on nearby mobs and changes bot loot behavior.
3. **Only guard `sendMoney`.** Stops the exception but leaves collateral mob kills and pickup work in place.

## Files expected to change

- `SRC/src/nro/models/Bot/Bot.java`: expose a small `isBossHunter()` predicate for bot type 2.
- `SRC/src/nro/models/services/SkillService.java`: skip secondary mob hits for boss-hunter special skills.
- `SRC/src/nro/models/services/Service.java`: guard `sendMoney` for null player/session.

## Verification

- Compile the changed Java sources against the current server JAR and libraries.
- Inspect the resulting diff to confirm regular-player special-skill mob hits and boss-target attacks remain unchanged.
- Do not run the server. The user will confirm behavior by running it.
- Do not add or run tests unless the user requests testing.

## Acceptance criteria

- A boss hunter can continue attacking its selected boss with special skills.
- Those special skills do not damage ordinary mobs around the boss.
- A call to `sendMoney` with a null player or null session does not throw or print a stack trace.
- Existing connected players still receive money updates.
