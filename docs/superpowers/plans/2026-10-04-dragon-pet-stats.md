# Dragon pet stats implementation plan

**Goal:** Match the seven golden-egg dragon variants to the user's stats in SQL and source, including working effects and 10% permanent rewards.

**Architecture:** A focused DragonPetCatalog owns default profiles and loads matching rows from dragon_pet_config at server startup. ID 1765–1771 maps to stars 7–1 based on the existing icons. The egg handler applies exactly one profile, non-trade option 30, and option 93 only for temporary rewards. NPoint aggregates option 236; Mob applies the luck multiplier to its existing item drop rolls. Existing pet items are not migrated automatically.

**Tech Stack:** Java 17, JDBC/MySQL, existing standalone Java tests, Python packaging and resource audit.

- [x] Write failing tests for exact profiles, expiry boundaries, equip/remove effects and luck/drop integration.
- [x] Implement DragonPetCatalog; wire Manager startup and golden egg rewards.
- [x] Implement option 236 aggregation/reset and multiplier in Mob; retain existing combat handlers for options 94, 108 and 106.
- [x] Add SQL config migration and update seven template names in database and both maintained SQL dumps, with snapshots/rollback and file backups.
- [x] Increase item catalog version; compile changed classes, package preserving all unrelated JAR entries, run resource and regression tests.
- [x] Gracefully maintain/save server, archive old JAR with SHA-256 metadata, apply SQL, install and restart, verify runtime and document final profiles.

User choices: permanent chance 10%; temporary duration uniformly random 1–30 days; luck +20% means a relative 1.2 multiplier on monster item drops, stacking with existing bonuses. Preserve previous VIP reward removals and ghost-rider icon.
