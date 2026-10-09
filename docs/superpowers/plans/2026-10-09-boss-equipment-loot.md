# Requested boss equipment loot

Approved rates: sealed shop level 9–12 5%, divine 1%, sealed divine 0.5% per rewarded boss defeat. Town Cell: sealed shop only. Arena Cell, Black Goku including Zamasu, Cumber, Cooler and Drabura 1/2/3: all three categories. Mabu 14h: divine categories only. Chilled has IDs but no implementation/factory/spawn, so report that blocker without creating a new boss.

Audit: town Cell drops gold/dragon balls; Mabu14h has points/tasks only; arena Cell/Black Goku/Zamasu/Cumber/Cooler/Drabura have plain shop equipment, native divine and dragon balls/gold. No selected boss currently grants seals 34–36. No active SQL custom drop overrides found. Retain plain equipment and all non-equipment drops. Replace native divine construction only inside selected boss reward scopes so the approved rare rates do not stack with old 5% rates. Use existing sealed pool/options builder; each category rolls independently, max one of each per defeat. Shared claimed gate prevents duplicate reward callbacks; admin replace policies suppress the default pools, append still adds configured loot. Quest support gets no equipment.

- [ ] Add deterministic failing tests for rates, pools, seals, native suppression, no duplicate callbacks and exclusions.
- [ ] Reuse the existing equipment builder and add a scoped boss equipment policy to BossAdminDropService.
- [ ] Verify all target IDs/forms, boundary rolls, owner/pet ownership, native gold/dragon balls, overrides and existing sealed/drop/achievement regressions.
- [ ] Compile and package only changed classes; record audits/metadata and catalog summaries. Deploy only after allowed saved maintenance with backup and hash checks.
