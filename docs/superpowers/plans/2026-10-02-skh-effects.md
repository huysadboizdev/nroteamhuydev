# SKH gameplay effects implementation plan

**Goal:** Apply the combat and normal monster loot effects described by team2026.sql, following the user's approval to implement them.

**Architecture:** Keep effects in the existing NPoint, SkillService, Mob and MobMe execution paths. Share Kaioken HP/KI costs between eligibility checks and consumption. Gohan multiplies random loot gate chances by 2.5 after existing bonuses and multiplies generated gold quantities by 1.3 using long arithmetic; item selection, rarity rolls, guaranteed task rewards and boss rewards remain in their existing paths.

**Tech stack:** Java 17, offline main-based regression tests, javac with existing dependency JARs. Workspace has no Git repository; verification outputs go in .analysis.

- [x] Add SKHEffectTest modes for damage, critical, Kaioken, explosion, summoned creature lifecycle, loot, and existing sets. Run each mode before implementation and record the expected failures.
- [x] NPoint.java: Picolo x2 with integer saturation; Pikkoro x2; Nail Masenko +80% once; apply Nail critical damage bonus and activate Kaio +10 critical points at >=2 pieces.
- [x] SkillService.java: shared Kaioken costs (HP 10/5/3% at <4/4/5 pieces; KI 100/50/30% of normal cost), preserve other skill mana modes, explosion radius +200 at >=2 pieces, use long explosion damage arithmetic.
- [x] MobMe.java: live owner equipment determines bonus damage and invulnerability, lifetime remains protected only at 5 pieces; expired creature dies after removing a piece; preserve map movement and owner-death cleanup.
- [x] Mob.java: Gohan loot rolls preserve fractional chances; gold bonus applies once before ItemMap creation, capped at Integer.MAX_VALUE. Keep existing clover and option 236 bonuses.
- [x] Compile all production/test source; execute new modes and all existing regressions, update audit notes with actual results. No server restart or deployment JAR replacement.
