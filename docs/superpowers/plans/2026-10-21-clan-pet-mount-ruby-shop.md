# Guild Pet and Mount Ruby Shop Implementation Plan

**Goal:** Move the guild Pet and Ván Bay shop items to two Santa ruby shop tabs at 50 ruby each and shut down purchases from the guild shop.

**Architecture:** Keep existing item IDs, item templates, icons, and part data. Add idempotent SQL that creates two Santa tabs, copies the 21 catalog rows as ruby sales, and disables the old rows. Hide the guild shop NPC option and reject legacy guild shop purchases server-side; extend Santa's allowlist with a fixed 50 ruby price for these IDs.

**Tech Stack:** Java 17 server, MySQL/MariaDB SQL migrations, existing item icon and part data pipeline.

---

### Task 1: Move the catalog in SQL

**Files:**
- Create: `SRC/sql/migrations/20261021_clan_pet_mount_ruby_shop.sql`
- Modify: `SRC/sql/team2026.seed.sql`

- [x] Add Santa tabs named `Pet` and `Ván Bay`, scoped to the `SANTA_HONG_NGOC` shop (`npc_id=39`, `type_shop=3`).
- [x] Copy all 15 items from source tab 61 and all 6 items from source tab 62 to the matching Santa tabs, preserving item options while setting `is_sell=1`, `type_sell=3`, `cost=50`, and `icon_spec=7743`.
- [x] Make reapplication safe by matching shop/tab names and item template IDs; update existing target rows instead of duplicating them.
- [x] Set source rows in tabs 60, 61, and 62 to `is_sell=0` so the full old guild store is inactive while the catalog remains available for rollback.

### Task 2: Disable the guild shop and allow the Santa sale

**Files:**
- Modify: `SRC/src/nro/models/npc_list/GiuMaDauBo.java`
- Modify: `SRC/src/nro/models/shop/ShopService.java`

- [x] Remove only the “Cửa Hàng Bang hội” menu option and its dispatch from Giu-ma Đầu Bò.
- [x] Reject opening and purchasing when the active shop tag is `SHOP_CLAN`, including requests from a previously opened shop window.
- [x] Add the 21 Pet and Ván Bay template IDs to Santa ruby shop's allowlist and require a cost of exactly 50 ruby for those IDs.
- [x] Preserve all existing Santa ruby item rules and leave Capsule Bang generation and non-shop uses unchanged.

### Task 3: Review the result

- [x] Inspect the diff and confirm only the planned NPC, shop, seed, migration, and plan files changed.
- [x] Confirm the source contains 15 Pet rows plus 6 ride rows, all referenced SQL templates and ride parts, and all 84 icon files across zoom levels; review the migration's copy, price, currency, and deactivation statements.
- [x] Do not build a JAR/APK or run automated tests unless requested; the migration still needs application to a database and the server needs a user-requested build/deployment.
