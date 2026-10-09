# Disciple companion implementation plan

Goal: Let Uub, Kid Beerus and Kid Jiren equip supported companion items in body slot 7 (eighth slot), with their own companion and item bonuses.

Architecture: Reuse NewPet with the disciple as its owner. Extract the existing companion model mappings into a catalog shared by equipment eligibility and login reconstruction. Keep mounts rejected. Use typePet for disciple eligibility. Suspend companion visibility while its owner is absent, dead, at home or fused. Preserve player companions and inventory transfer rules.

- [x] Add `SRC/test/nro/models/services/DiscipleCompanionTest.java`: exercise real inventory transfers, VIP versus ordinary disciples, unsupported type-27 consumables, replacement, full bags, reconstruction, map transitions and suspension. Run against current source and confirm expected assertion failures.
- [x] Add `SRC/src/nro/models/item/CompanionCatalog.java` from existing Player.sendNewPet mappings. Use it in `Player.sendNewPet` and `InventoryService.putItemBody`; VIP eligibility reads `Pet.typePet`.
- [x] Update successful disciple equip/unequip to create/retire the owner's NewPet and recalculate disciple stats. Reconstruct in Pet.update; preserve items on rejected transfers. Correct pet flag lookup to slot 8 and type 11 so slot-7 companion items are never rendered as back accessories.
- [x] Update NewPet visibility and follow guards; verify replacement, logout disposal, fusion/home/death suspension, follow and map transitions. Sync type during PetService creation.
- [x] Compile all Java sources with `javac --release 17 -encoding UTF-8 -cp SRC/lib/*`; compile and run DiscipleCompanionTest and TigerPetAuditTest. Review scoped diff and verify deployment candidate matches changed classes.
- [x] User approved maintenance/deployment. Verified server already stopped before replacing JAR, archived prior JAR with SHA-256 metadata, installed candidate, started one server JVM and verified listener 14445. Both regression suites passed directly on installed SRC/20.jar.

Scope: no client wire-format changes; no database schema changes. Existing slot 8 stays shared by books/back accessories. Preserve unrelated dirty workspace changes.
