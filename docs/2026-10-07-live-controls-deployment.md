# Live controls deployment — 2026-10-07

Installed after data-saving maintenance at 18:43 Bangkok; server restarted as PID 9452 and opened port 14445. Application JAR SHA-256: `52b1fd766f4f5adf6d19c6261254c0362c6f1fe765537fce9e17a702e053b6ed`.

Immediate old JAR: `SRC/jar-backup/20-before-web-manager-20261007-184305.jar`, SHA-256 `f902d3d718cde8905b01a61cf3040db89e53fea987b7caced6635e0c1612365c`. SQL snapshot and web archive are in `.analysis/web-manager-deploy-20261007-184305`. Deployment and verification metadata are `.analysis/web-manager-deployment-latest.json` and `.analysis/web-manager-verification-latest.json`.

Implemented: durable live EXP setting, safe long EXP arithmetic, system world-channel notices, account-scoped atomic giftcode receipts and pending-logout recovery, separate boss reload settings, training-boss cleanup and HP/damage overflow fixes. Gift catalog reload preserves account claim history even after catalog deletion/recreation.

Verification: 161 Java test groups on compiled classes and candidate JAR; 41 installed LiveSync test groups; 44 runtime class hashes; six read-only catalog staging checks; isolated SQL tests for canonicalization, legacy backfill, idempotent migrations, quota race, rollback, uncertain commit and end-to-end rewards. PHP API/queue/catalog/gift/boss validation, lint and seven inline scripts passed. Runtime EXP remains 3 from configuration because no persisted override has been submitted. Queue has no pending/processing rows; one historical failure remains untouched.

Limits: boss overrides apply through generic Boss.initBase; mini bosses with generated IDs and bosses calculating stats from the player retain specialized calculations. Web page states this explicitly. Special gift rewards that change charms, permanent pets or map-ball state are rejected before consuming quota; ordinary items and currency are supported. No real-player rewards or trial world broadcasts were issued. Missing tile warnings for maps 78, 95, 101, 121, 125, 130 and 134 predate this deployment.

Restore only after data-saving maintenance and confirmation the game JVM has stopped. Restore the immediate backup JAR and preserve current player data; do not blindly import the pre-deployment SQL snapshot over newer progress.
