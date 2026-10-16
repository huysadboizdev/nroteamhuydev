# Missing one-damage mob hits — 2026-10-16

## Cause

The server sends the mob's updated HP and the hit amount in command `-9`. Both client lanes (`Game1/Controller.cs` and `Game2/Controller.cs`) updated HP, then wrapped all hit feedback in `if (num204 != 1)`. When damage equaled exactly one, the client skipped the hit animation, crit/effect fields, and floating damage text. The HP bar could move by one while no number appeared, which looked like a missed punch.

The server also had two low-HP integer-rounding edges. The training-dummy limit used `maxHp / 10`, and the level-mob damage calculation used 10%; values below 10 could turn a positive hit into zero. These are separately covered and now retain one damage for a positive hit. Zero-damage misses remain zero.

## Changes

- Game1 and Game2 process positive `-9` mob hits, including damage 1, while keeping zero-damage miss feedback.
- `Mob.injured` preserves a minimum of one in both low-HP damage calculations only for positive incoming damage.
- Cooldown, hit validation, full-HP protection, and XP logic are unchanged.

## Verification

- Before the client change, `MobOneDamageDisplayChecks.ps1` failed in both game lanes on the `num204 == 1` skip.
- After the change, that check passed for both lanes and confirmed zero-damage miss feedback remains.
- Server `MobPunchDeliveryTest` passed through `useSkillForMob` → `playerAttackMob` → `Mob.injured`; low-HP one-damage targets lost one HP and emitted a `-9` packet containing damage 1.
- Seven `SkillCastLifecycleTest` modes passed.
- Manual attack, attack result, massacre range/animation, and outgoing queue client checks passed.
- Unity 2022.3.62f2 Windows build succeeded with zero errors and 64 warnings. The project verification logged 169,616 high-speed skill checks and the equipment aura, black-power aura, and NPC image checks passed.
- Installed `SRC/20.jar` SHA-256: `4e04372ef57530b4d6772d10132c3a6281c9fe90b864c35b2212f730e2324d4a`. Previous JAR is preserved at `SRC/jar-backup/20-before-mob-minimum-positive-damage-20261016-160631-968.jar`; metadata is alongside it.
- Installed Windows client in `MODNROVEGETA/Windows`; verified 132 files. ZIP SHA-256: `20203e9a10ce2c11089597c1946ff7692faf6fb1cf7b95dec9d7525be140a835`. Previous Windows directory is preserved as `Windows-backup-one-damage-display-20261016-161553-907`.

## Limits

No NRO client was running to reproduce the effect against the live server after installation. The Windows Computer Use connection was unavailable, so the supplied MP4 was not played in this session. The source handler condition reproduces the reported exact-one-damage display loss. `SRC/20.jar` was updated in the workspace; no remote server JAR was changed.
