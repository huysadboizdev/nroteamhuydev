# Ruby octopus deployment ? 2026-10-07

Existing grilled octopus (881, icon 8061) increases critical hit damage by 5 percent, not critical hit chance. Added to existing Santa ruby shop tab 64 for 5 ruby (type_sell 3, icon_spec 7743). ShopService validates the fixed price for all three seafood IDs 880?882. Existing duration rules remain: each dose adds ten minutes, maximum 500 minutes, one seafood variant active at a time, repeated doses do not increase the percentage.

Red test on the previous installed JAR rejected ID 881. Candidate passed 21 test groups covering actual purchase, currency, insufficient funds, full bag, forged price, existing shop routes, rollback, concurrency, all food duration caps, persistence, and seafood stats. The installed JAR passed RubyFoodShopTest, octopus-stats (critical damage +5, critical chance and base damage unchanged, second dose twenty minutes), and food-real for all eight food IDs. Migration ran twice in an isolated catalog clone with exactly one row per food and original shop items preserved. Production SQL confirms all three seafood items cost 5 ruby. Four SQL exports include the migration.

Data-saving maintenance completed before replacing the JAR. SQL snapshot and old JAR hashes verified. Restarted PID 19416 listening on port 14445. Only SRC/20.jar remains in the application run directory.

Backup: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-octopus-20261007-190456.jar`.
Old SHA-256: `61ee4608a7116dabb2002b6a9cc756c654a23c7a3622c6134b020194f40db909`.
New SHA-256: `99d036367fc5749732b6cf4ac1d74c7c98f25e5c4dd8a89d375d787f5f75606b`.
Metadata: `.analysis/octopus-deployment-latest.json`.
Rollback: maintenance/save first, restore immediate JAR backup, disable octopus sale in Santa ruby shop, preserve current player data.
