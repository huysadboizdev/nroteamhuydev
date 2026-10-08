# Mabu loot and divine power requirement implementation plan

**Goal:** Mabu drops 90% sealed shop equipment, 8% ordinary divine, 2% sealed divine; eggs 5%, Killis 3%. Divine items must not display or enforce zero power requirements.

**Architecture:** Keep shared equipment pools and build logic, add instance-specific category thresholds for Mabu. Repair missing or nonpositive divine option 21 through ItemDefaultOptions.complete, already called when restoring bags, boxes and equipped items. Use 15 billion for missing requirements; preserve existing positive requirements and all stats/seals. The client already reads option 21 correctly, so no client patch or template SQL change is needed.

**Tech Stack:** Java 17-compatible server, MySQL, existing Unity Game1/Game2 clients.

- [x] Add and run failing DivinePowerRequirementTest against current JAR: missing option 21 must still enforce 15 billion and normalization must add the option without altering existing 17-billion requirements.
- [x] Update ItemDefaultOptions.requiredPower/complete for template IDs 555–567. For absent/nonpositive option 21 add `(21,15)`; never clear stats/seals.
- [x] Add MabuLootBalanceTest: actual Mabu equipment helper must produce exactly 90/8/2 categories across all 100 rolls; configured rare rates must be 5/3. Keep default helper 60/30/10 for hunting trios.
- [x] Add instance category thresholds to SealedBossEquipmentLoot, retain static build default 60/30/10, configure Mabu with `(90,8)` and constants 5/3.
- [x] Run full build/regression and isolated SQL checks via `.analysis/build_mabu_balance_release.py --db`, then run the same Java groups against candidate JAR.
- [x] Independently review power normalization and rate isolation; fix findings before installation.
- [x] Maintenance-save and verify JVM exit; snapshot SQL and backup SRC/20.jar with hashes; install tested candidate and restart hidden server.
- [x] Read-only runtime probe confirms Mabu configuration, corrected divine power requirement and boss notice; verify backup/new hashes and compiled class equality.

No SQL player-data rewrite is needed: saved inventory loads normalize items after decoding. Offline players are repaired at next login. Existing inventory options remain intact and are saved by the normal player save process.

Review found a second visible zero string in Item.getContent. Added a failing content assertion, then made divine content use requiredPower. Final source build: 97 Java groups and 3 isolated SQL groups PASS. Candidate and deployment verification follow.

Deployed 2026-10-08 12:16 Bangkok. Runtime PID24416: Mabu ACTIVE, equipment90/8/2, egg5%, Killis3%, divine missing requirement normalized to15b and content correct. All817 compiled classes match installed JAR; backup/new SHA-256 verified.
Backup: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-mabu-balance-release-20261008-121620.jar`
New SHA-256: `08a137803c32317fa32bb1d64c3be5fb7229fad506dbc5e3e6df8121e6760a1b`
