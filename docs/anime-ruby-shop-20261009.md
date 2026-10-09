# Anime equipment / ruby shop release

Status: DEPLOYED and verified after user approval on 2026-10-09. Final server PID17088 listens14445.

Five items, 200 ruby each, Santa ruby shop (shop 37 / tab 64):
- 2019 Kurama pet: options 50=13,77=13,103=13,204=13,14=25,30=0.
- 2026 Naruto: options 77=25,6=50000,94=30,30=0.
- 2027 Sasuke: options 50=25,0=30000,14=15,30=0.
- 2030 Sharingan flag: options 50=20,77=20,103=20,95=20,96=20,14=40,30=0.
- 2039 Akatsuki: options 50=22,77=22,103=22,101=100,160=70,14=10,30=0.

Duration: 20% permanent; 80% 7-30 days. Restoring/copying/saving does not reroll. All five are non-tradable. Existing saved explicit stats preserved when completing missing defaults.
Pairs: Sasuke in costume slot 5 with flag slot 8; Naruto in costume slot 5 with Kurama slot 7. Exactly one +2% HP/KI/attack bonus while active and equipped. Expired accessory does not qualify.
Boss bonus: option204 implemented for both option aggregation paths and all getDameAttack branches, using actual Boss target and saturated arithmetic; PvP/ordinary mobs unchanged.
Akatsuki: source parts 2012/2013/2014 mapped to 2134/2135/2136; 120 staged PNGs across x1-x4, conflict remapping, avatar and shop icon included.
Cache versions: vsData 16, vsItem 22. Goku item2020 unchanged.

Verification: full Java source compile (Lombok processing enabled), exact profiles, all 100 permanence rolls, all 24 day outcomes, restore retention, pair removal/expiry/repeated recalculation, boss bonus/overflow, eight actual shop scenarios on candidate JAR. Isolated SQL apply/rollback, five exact option profiles, three parts, one avatar, 120 PNG hashes/decode all passed. Rollback test intentionally raises grant failure; test confirms bag/wallet restored.

Candidate: C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-anime-shop-candidate-20261009-115931.jar
SHA256: 04d47d10acdb5ff2c5d3f87f0a2637ffd9f5077b308a3298250a0f9dc00c217e
Active JAR SHA256: 3a6fd3fb93c15d8134a6fef47fc1586db86eed673b55dcca041b29a6687b41d4
Installer: .analysis/anime-shop/install.py (saved maintenance, timestamped JAR and SQL backups, hash guards, rollback, hidden restart).
Migration: SRC/sql/migrations/20261009_anime_ruby_shop.sql
Both seed SQLs include the migration.

After explicit approval: execute installer; verify process/port, SQL rows/options, deployed classes/hash, all installed icon hashes and server startup errors. In-game sprite/visual inspection is still needed.

Deployment completed: saved maintenance twice; both old application JARs archived with timestamp/hash; SQL snapshot before initial migration. Startup verification found sparse SQL item IDs and corrected Manager indexing with inert gap entries (type -1); server getTemplate rejects gap IDs. This fixes both real server lookups and client positional template IDs. Full regression and read-only ShopDAO integration against deployed SQL pass.
Final SHA256: 272538b29d97416b26694be1edf76d259a79c2ef5ec43011dad4c6a375f029ee
Final backup: C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-anime-shop-fix-20261009-120529.jar
120 installed PNG hashes and five exact SQL option profiles verified. Existing map tile warnings for IDs78/95/101/121/125/130/134 remain from prior releases. In-game visual inspection remains for user; reconnect client to refresh versions16/22.
