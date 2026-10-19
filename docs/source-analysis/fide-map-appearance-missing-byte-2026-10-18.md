# Fide missing from the map: appearance packet diagnosis

Checked the running server and Unity client on 2026-10-18 (Asia/Bangkok).

## Confirmed cause

The server's `Zone.infoPlayer()` sends appearance message `-5` through the hat field, but both Game1 and Game2 clients unconditionally read another byte into `char10.isTichXanh` before adding the character to `GameScr.vCharInMap`. That read throws at EOF and the enclosing empty catch discards the appearance. `Service.sendInfoCharMoiToMe()` has the same omission for bots.

This is a packet layout mismatch affecting map appearances, rather than missing Fide artwork or a boss membership failure.

## Live evidence

- Server PID 20440: Fide ID -28, map 80, zone 0, ACTIVE, 50,000,000/50,000,000 HP, present exactly once in the zone's boss and humanoid lists, one player in the zone.
- Unity client debugger: Game1 map 80, zone 0, player 1367, `vCharInMap` count 0. Both `AnPlayer` and `notPaint` false. Fide parts 183–191 are loaded.
- Captured the actual `-5` snapshot from the running `Zone.infoPlayer()` into a detached capture receiver: 67 bytes, with no badge byte.
- Replayed that snapshot to the existing player after appending a single zero badge byte. No server restart, boss respawn, movement, HP change, or class replacement was performed.
- A fresh client debugger read at 20:53:17 local time showed `vCharInMap` count 1, Fide ID -28, outfit 183/184/185, HP 50,000,000, `isHide=false`, `isInvisiblez=false`, `isTeleport=false`.

Evidence is retained in `.analysis/fide-live-diagnosis/` and `.analysis/all-boss-visibility/fide-live-20261018.tsv`. The Windows UI helper was unavailable; confirmation used the running client's actual character collection and visibility flags, rather than a screenshot.

## Source correction and verification

- Append the `isNewMember` badge byte to `Zone.infoPlayer()` and `Service.sendInfoCharMoiToMe()`, matching the clients' existing `-5` contract.
- `CharacterAppearanceWireTest` exercises the real appearance writers, reads every field through the badge, and requires exact packet consumption. Covers all three Fide outfits and the bot writer with both badge values (8 cases).
- Before correction, the test failed: `client requires exactly one badge byte after hat; remaining=0`.
- After correction, both changed Java classes compiled and all 8 appearance cases passed.
- Related `BossMembershipTest` cases passed: `preassigned`, `repeat`, `partial`, `cellwire`, `offline`, `bossmove`.
- Verification compiled individual classes and test/tool code only. No application JAR/APK/EXE was built or replaced. Active `SRC/20.jar` SHA-256: `6521fb129ed8177ca774e7ecd2ecf8d2fa18bb98caa4865d09efa1ffed0c7ba2`.

## Deployment status

The replay repaired the currently connected client's missing Fide entry. The source correction has not been deployed into the running server JAR. A subsequent map reload, reconnect, or boss appearance can encounter the old packet again until an explicitly authorized application build/update is performed under AGENTS.md.
