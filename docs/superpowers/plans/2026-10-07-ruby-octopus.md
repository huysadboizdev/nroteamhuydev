# Ruby octopus implementation plan

**Goal:** Add existing grilled octopus (881, icon 8061) to the Santa ruby shop for 5 ruby per item, following the approved food shop behavior.

**Architecture:** Extend existing ShopService food allowlist and price validation. Reuse current SelfBuffRules and NPoint critical damage calculation unchanged. Add a separate idempotent SQL migration in the existing shop tab.

- [x] Extend RubyFoodShopTest to all three food IDs and confirm 881 fails on installed JAR.
- [x] Verify octopus grants 5 percent critical damage, preserves critical chance, and repeated doses extend time without multiplying stats.
- [x] Allow ID 881 and require cost 5 in ShopService.
- [x] Test SQL migration twice against cloned catalog tables; preserve the existing eleven shop items.
- [x] Build candidate and run food and Santa shop regression tests.
- [x] Save data through maintenance, snapshot SQL, move old JAR to timestamped backup and verify SHA-256 before installing candidate.
- [x] Restart server, verify SQL prices, installed JAR tests, backup hash, and listener.
